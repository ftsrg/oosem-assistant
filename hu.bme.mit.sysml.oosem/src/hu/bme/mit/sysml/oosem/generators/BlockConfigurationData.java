package hu.bme.mit.sysml.oosem.generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.ConfigurationElement;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMBlock;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMElement;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMFeature;
import hu.bme.mit.sysml.oosem.util.OOSEMUtils.OOSEMBlockType;

public class BlockConfigurationData {
	public BlockConfigurationData(OOSEMBlock block) {
		configStructure = new ConfigurationStructure(block);
		nameMapping = new NameMapping(configStructure.getConfigRoot());
	}

	public ConfigurationStructure.BlockConfiguration getBlockConfigurationRoot() {
		return configStructure.getConfigRoot();
	}

	public NameMapping getNameMapping() {
		return nameMapping;
	}

	private final ConfigurationStructure configStructure;
	private final NameMapping nameMapping;


	public class ConfigurationStructure {

		public ConfigurationStructure(OOSEMBlock block) {
			configRoot = new BlockConfiguration(block, 2);
		}

		public BlockConfiguration getConfigRoot() {
			return configRoot;
		}

		private final BlockConfiguration configRoot;

		public abstract class ConfigurationElement {
			public ConfigurationElement(OOSEMElement oosemElement) {
				this.oosemElement = oosemElement;
			}

			public boolean isSelected() {
				return selected;
			}
			public void setSelected(boolean selected) {
				this.selected = selected;
			}
			public OOSEMElement getObject() {
				return oosemElement;
			}
			public abstract Set<ConfigurationElement> getChildElements();

			private final OOSEMElement oosemElement;
			private boolean selected = false;
		}

		public class BlockConfiguration extends ConfigurationElement {
			public BlockConfiguration(OOSEMBlock oosemBlock, int configurationDepth) {
				super(oosemBlock);

				if(configurationDepth > 2) {
					throw new IllegalArgumentException("The maximum supported 'configurationDepth' is 2.");
				}

				oosemBlock.getSubsystems().stream()
					.filter(OOSEMElement::passedValidation)
					.forEach(p -> {
						var conf = new FeatureConfiguration (p, configurationDepth - 1);
						if(p.getLowerMultiplicity() == 1 && p.getHigherMultiplicity() == 1) {
							mandatorySubsystems.add(conf);
						} else if(p.getLowerMultiplicity() == 0 && p.getHigherMultiplicity() == 1) {
							optionalSubsystems.add(conf);
						}
					});
			}

			public Set<FeatureConfiguration> getMandatorySubsystems() {
				return mandatorySubsystems;
			}
			public Set<FeatureConfiguration> getOptionalSubsystems() {
				return optionalSubsystems;
			}
			@Override
			public Set<ConfigurationElement> getChildElements() {
				var res = new HashSet<ConfigurationElement>();
				res.addAll(mandatorySubsystems);
				res.addAll(optionalSubsystems);
				return res;
			}

			private final Set<FeatureConfiguration> mandatorySubsystems = new HashSet<FeatureConfiguration>();
			private final Set<FeatureConfiguration> optionalSubsystems = new HashSet<FeatureConfiguration>();

		}

		public class FeatureConfiguration extends ConfigurationElement {
			public FeatureConfiguration(OOSEMFeature oosemFeature, int configurationDepth) {
				super(oosemFeature);
				oosemFeature.getType().getAllChilds().stream()
					.filter(OOSEMElement::passedValidation)
					.forEach(p -> {
						if(!isIntegrateableWithoutHierarchy(p)) {
							if(configurationDepth != 1) {return;}
							if(p.getOOSEMBlockType() != OOSEMBlockType.DESIGN) {return;}
							if(!isDesignIntegrateableWithTwoLevelHierarchy(p)) {return;}
						}

						alternatives.add(new BlockConfiguration(p, configurationDepth));
					});
			}

			public Set<BlockConfiguration> getAlternatives() {
				return alternatives;
			}
			@Override
			public Set<ConfigurationElement> getChildElements() {
				var res = new HashSet<ConfigurationElement>();
				res.addAll(alternatives);
				return res;
			}

			private static boolean isIntegrateableWithoutHierarchy(OOSEMBlock block) {
				var blockType = block.getOOSEMBlockType();

				if(blockType == OOSEMBlockType.INTEGRATION) {return true;}
				if(blockType != OOSEMBlockType.DESIGN) {return false;}
				if(!block.getSubsystems().isEmpty()) {return false;}

				var integrations = block.getAllChilds().stream()
				.filter(q -> q.getOOSEMBlockType() == OOSEMBlockType.INTEGRATION)
				.collect(Collectors.toSet());
				if(integrations.isEmpty()){return true;}

				return false;
			}

			private static boolean isDesignIntegrateableWithTwoLevelHierarchy(OOSEMBlock block) {
				for(var subsystem : block.getSubsystems()) {
					boolean integrateableWithOneLevel = false;
					for(var realization : subsystem.getType().getAllChilds()) {
						if(isIntegrateableWithoutHierarchy(realization)) {
							integrateableWithOneLevel = true;
							break;
						}
					}
					if(!integrateableWithOneLevel) {return false;}
				}

				return true;
			}

			private final Set<BlockConfiguration> alternatives = new HashSet<BlockConfiguration>();
		}

	}

	public class NameMapping {
		public NameMapping(ConfigurationStructure.BlockConfiguration root) {
			var rootInput = new HashMap<ConfigurationElement, String>();
			rootInput.put(root, "");
			processElements(rootInput);
		}

		public ConfigurationElement mapIdToElement(String id) {
			return idToElement.get(id);
		}

		public String mapElementToId(ConfigurationElement element) {
			return elementToId.get(element);
		}

		public int getElementCount() {
			return idToElement.keySet().size();
		}

		public Set<String> getIds() {
			return idToElement.keySet();
		}

		private final Map<String, ConfigurationElement> idToElement = new HashMap<String, ConfigurationElement>();
		private final Map<ConfigurationElement, String> elementToId = new HashMap<ConfigurationElement, String>();


		private void processElements(Map<ConfigurationElement, String> elementsWithParenID) {
			if (elementsWithParenID.keySet().isEmpty()) return;

			var nameCandidatesWithElements = collectElementsToNameCandidates(elementsWithParenID);
			assignNamesToElements(elementsWithParenID, nameCandidatesWithElements);

			processElements(prepareInputForNextLayer(elementsWithParenID));
		}

		private HashMap<String, List<ConfigurationElement>> collectElementsToNameCandidates(Map<ConfigurationElement, String> elementsWithParenID) {
			var nameCandidatesWithElements = new HashMap<String, List<ConfigurationElement>>();

			for(var element : elementsWithParenID.keySet()) {
				var elementsToID = nameCandidatesWithElements.get(element.getObject().getName());

				if(elementsToID == null) {
					elementsToID = new ArrayList<ConfigurationElement>();
					nameCandidatesWithElements.put(element.getObject().getName(), elementsToID);
				}

				elementsToID.add(element);
			}

			return nameCandidatesWithElements;
		}

		private void assignNamesToElements(Map<ConfigurationElement, String> elementsWithParenID,HashMap<String, List<ConfigurationElement>> nameCandidatesWithElements) {
			for(var nameCandidate : nameCandidatesWithElements.keySet()) {
				var elementsToCandidate = nameCandidatesWithElements.get(nameCandidate);

				if(elementsToCandidate.size() == 1 && idToElement.get(nameCandidate) == null) {
					registerElementWithName(elementsToCandidate.getFirst(), nameCandidate);
				} else {
					for(var element : elementsToCandidate) {
						registerElementWithName(element, nameCandidate + "\\" + elementsWithParenID.get(element));
					}
				}
			}
		}

		private Map<ConfigurationElement, String> prepareInputForNextLayer(Map<ConfigurationElement, String> elementsWithParenID) {
			Map<ConfigurationElement, String> nextLayerInput = new HashMap<ConfigurationElement, String>();

			for(var element : elementsWithParenID.keySet()) {
				for(var child : element.getChildElements()) {
					nextLayerInput.put(child, elementToId.get(element));
				}
			}

			return nextLayerInput;
		}

		private void registerElementWithName(ConfigurationElement element, String name) {
			idToElement.put(name, element);
			elementToId.put(element, name);
		}
	}
}