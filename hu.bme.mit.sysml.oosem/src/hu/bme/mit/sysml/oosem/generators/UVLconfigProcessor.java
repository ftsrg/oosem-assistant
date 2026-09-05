package hu.bme.mit.sysml.oosem.generators;

import java.util.HashSet;
import java.util.Set;

import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.BlockConfiguration;
import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.FeatureConfiguration;
import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.NameMapping;
import hu.bme.mit.sysml.oosem.generators.BlockGenerationData.RefinementData.RefinementConfiguration;
import hu.bme.mit.sysml.oosem.generators.BlockGenerationData.RefinementData.RefinementConfiguration.RefinementWorkflow;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMBlock;
import hu.bme.mit.sysml.oosem.model.elements.OOSEMFeature;
import hu.bme.mit.sysml.oosem.model.project.interfaces.OOSEMProject;
import hu.bme.mit.sysml.oosem.util.OOSEMUtils.OOSEMBlockType;
import hu.bme.mit.sysml.oosem.util.readers.uvl.ConfigurationSelection;

public class UVLconfigProcessor {
	public UVLconfigProcessor(OOSEMProject project, BlockConfigurationData data, ConfigurationSelection selection) {
		this.selection = selection;
		if(!preliminaryValidation(data, selection)) {System.err.println("Failed preliminaryValidation");return;}
		process(data, selection);
		if(!postGenerationValidation()) {System.err.println("Failed postGenerationValidation");return;}
		blockGenerationData = new BlockGenerationData(project, (OOSEMBlock)data.getBlockConfigurationRoot().getObject() ,OOSEMBlockType.INTEGRATION);
		for(var r : subsystemRefinements) {	
			blockGenerationData.registerSubsystemRefinement(r);
		}
	}
	
	public BlockGenerationData getBlockGenerationData() {
		return errors.isEmpty() ? blockGenerationData : null;
	}
	
	public Set<String> getErrors() {
		return errors;
	}
	
	private BlockGenerationData blockGenerationData;
	private ConfigurationSelection selection;
	private String rootId;
	private Set<RefinementConfiguration> subsystemRefinements = new HashSet<>();
	private Set<String> processedIds = new HashSet<>();
	private Set<String> errors = new HashSet<String>();
	
	private boolean preliminaryValidation(BlockConfigurationData data, ConfigurationSelection selection) {
		var nameMapping = data.getNameMapping();
		var ElementCountDelta = nameMapping.getElementCount()-selection.getElementCount();
		var ids = nameMapping.getIds();

		switch(ElementCountDelta) {
		case 0:
			if(ids.containsAll(selection.getSelectedIds()) && ids.containsAll(selection.getUnselectedIds())) {
				return true;
			}
		case 1:
			if(ids.containsAll(selection.getSelectedIds()) && ids.containsAll(selection.getUnselectedIds()) && !selection.hasElement(nameMapping.mapElementToId(data.getBlockConfigurationRoot()))) {
				return true;
			}
		default:
			errors.add("Config file does not match model structure.");
			return false;
		}
	}
	
	private void process(BlockConfigurationData data, ConfigurationSelection selection) {
		var mapping = data.getNameMapping();
		var root = data.getBlockConfigurationRoot();
		rootId = mapping.mapElementToId(root);
		processedIds.add(rootId);
		subsystemRefinements = buildRefinements(root, mapping, selection);
	}
	
	private Set<RefinementConfiguration> buildRefinements(BlockConfiguration block, NameMapping mapping,
			ConfigurationSelection selection) {
		Set<RefinementConfiguration> refinements = new HashSet<>();
		
		for(var subsystem : block.getMandatorySubsystems()) {
			var refinement = buildFeatureRefinement(subsystem, mapping, selection, true);
			if(refinement != null) {
				refinements.add(refinement);
			}
		}
		for(var subsystem : block.getOptionalSubsystems()) {
			var refinement = buildFeatureRefinement(subsystem, mapping, selection, false);
			if(refinement != null) {
				refinements.add(refinement);
			}
		}
		
		return refinements;
	}
	
	private RefinementConfiguration buildFeatureRefinement(FeatureConfiguration feature, NameMapping mapping,
			ConfigurationSelection selection, boolean mandatory) {
		String featureId = mapping.mapElementToId(feature);
		OOSEMFeature refinedFeature = (OOSEMFeature) feature.getObject();
		
		if(!selection.getSelectedIds().contains(featureId)) {
			if(mandatory) {
				errors.add("Mandatory subsystem \"" + featureId + "\" was not selected.");
				return null;
			}

			return new RefinementConfiguration(refinedFeature, null, refinedFeature.getName(),
					RefinementWorkflow.UNINTEGRATED, "");
		}
		
		BlockConfiguration chosenAlternative = null;
		for(var alternative : feature.getAlternatives()) {
			String alternativeId = mapping.mapElementToId(alternative);
			if(selection.getSelectedIds().contains(alternativeId)) {
				if(chosenAlternative != null) {
					errors.add("Multiple alternatives selected for feature \"" + featureId + "\".");
					return null;
				}
				chosenAlternative = alternative;
			}
		}
		if(chosenAlternative == null) {
			errors.add("No alternative selected for selected feature \"" + featureId + "\".");
			return null;
		}
		
		OOSEMBlock chosenType = (OOSEMBlock) chosenAlternative.getObject();
		
		Set<RefinementConfiguration> subConfigs = buildRefinements(chosenAlternative, mapping, selection);
		
		processedIds.add(featureId);
		processedIds.add(mapping.mapElementToId(chosenAlternative));
		
		if(subConfigs.isEmpty()) {
			return new RefinementConfiguration(refinedFeature, chosenType, "",
					RefinementWorkflow.CHOOSE_EXISTING, "");
		}
		
		return new RefinementConfiguration(refinedFeature, chosenType, "",
				RefinementWorkflow.CONFIGURE, "", subConfigs);
	}

	private boolean postGenerationValidation() {
		processedIds.remove(rootId);
		
		Set<String> unreachedSelections = new HashSet<>(selection.getSelectedIds());
		unreachedSelections.removeAll(processedIds);
		unreachedSelections.remove(rootId);
		
		if(unreachedSelections.isEmpty()) {
			return true;
		}
		
		for(var id : unreachedSelections) {
			errors.add("Selected element \"" + id
					+ "\" was never integrated; its parent in the configuration tree may not have been selected.");
		}
		return false;
	}
}