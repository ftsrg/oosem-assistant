package hu.bme.mit.sysml.oosem.generators

import org.omg.sysml.lang.sysml.Type
import hu.bme.mit.sysml.oosem.generators.BlockGenerationData
import hu.bme.mit.sysml.oosem.util.OOSEMUtils.OOSEMBlockType
import hu.bme.mit.sysml.oosem.generators.BlockGenerationData.RefinementData.RefinementConfiguration.RefinementWorkflow
import hu.bme.mit.sysml.oosem.util.OOSEMUtils
import java.util.Set

class OOSEMBlockGenerator {
	def static String generate(BlockGenerationData data) '''
		package «data.blockName» {
		    private import OOSEM::OOSEM_Metadata::*;
		    private import «(data.subject.object as Type).qualifiedName»;
		    «generateImport(data.propertyRefinementConfigs.configurations)»
		    «generateImport(data.subsystemRefinementConfigs.configurations)»
		
		    «generateOOSEMType(data.tagetType)» «OOSEMGeneratorUtils.getSysMLType(data.subject.object as Type)» def «data.blockName» :> «(data.subject.object as Type).name» {
		        «generateFeatures(data.propertyRefinementConfigs)»
		        «generateFeatures(data.subsystemRefinementConfigs)»
		
		        //TODO: Auto-generated block skeleton
		    }
		    «generateBlockFramesForFeatures(data.propertyRefinementConfigs)»
		    «generateBlockFramesForFeatures(data.subsystemRefinementConfigs)»
		}
	'''
	
	def private static String generateImport(Set<BlockGenerationData.RefinementData.RefinementConfiguration> data) '''
		«FOR c : data»
			«IF c.workflow == RefinementWorkflow.CHOOSE_EXISTING || c.workflow == RefinementWorkflow.CONFIGURE»
				private import «(c.type.object as Type).qualifiedName»;
			«ELSEIF c.workflow == RefinementWorkflow.GENERATE_STUB»
				private import «(c.refinedFeature.type.object as Type).qualifiedName»;
			«ENDIF»
		«ENDFOR»
	'''
	
	def private static String generateOOSEMType(OOSEMBlockType type) {
		switch(type) {
			case SPECIFICATION: {
				return "#specification"
			}
			case DESIGN: {
				return "#design"
			}
			case INTEGRATION: {
				return "#integration"
			}
			default: {
				return ""
			}
		}
	}
	
	def private static String generateFeatures(BlockGenerationData.RefinementData data) '''
		«FOR c : data.configurations»
			«generateFeature(c)»
		«ENDFOR»
	'''
	
	def private static String generateFeature(BlockGenerationData.RefinementData.RefinementConfiguration c) '''
		«val hasName = c.name !== null && !c.name.isEmpty»
		«val isOptional = c.refinedFeature.lowerMultiplicity == 0 && c.refinedFeature.higherMultiplicity == 1»
		«IF c.workflow == RefinementWorkflow.CHOOSE_EXISTING »
			«OOSEMGeneratorUtils.getOOSEMMetadata(c.type.object as Type)» «OOSEMGeneratorUtils.getSysMLType(c.refinedFeature.object as Type)» «IF hasName»«c.name» «ENDIF»:>> «c.refinedFeature.name»«IF isOptional»[1]«ENDIF» : «c.type.name»;
		«ELSEIF c.workflow == RefinementWorkflow.GENERATE_STUB»
			«generateOOSEMType(OOSEMUtils.getTypeForNextPhase(c.refinedFeature.OOSEMBlockType))» «OOSEMGeneratorUtils.getSysMLType(c.refinedFeature.object as Type)» «IF hasName»«c.name» «ENDIF»:>> «c.refinedFeature.name»«IF isOptional»[1]«ENDIF» : «c.newTypeName»;
		«ELSEIF c.workflow == RefinementWorkflow.UNINTEGRATED»
			«generateOOSEMType(OOSEMUtils.getTypeForNextPhase(c.refinedFeature.OOSEMBlockType))» :>> «c.refinedFeature.name»[0];
		«ELSEIF c.workflow == RefinementWorkflow.CONFIGURE»
			«generateOOSEMType(OOSEMUtils.getTypeForNextPhase(c.type.OOSEMBlockType))» «OOSEMGeneratorUtils.getSysMLType(c.refinedFeature.object as Type)» «IF hasName»«c.name» «ENDIF»:>> «c.refinedFeature.name»«IF isOptional»[1]«ENDIF» : «c.type.name» {
			    «generateImport(c.subConfigurations)»
			
			    «FOR sc : c.subConfigurations»
			    «generateFeature(sc)»
			    «ENDFOR»
			
			    //TODO: Auto-generated block skeleton
			}
		«ENDIF»
	'''
	
	def private static String generateBlockFramesForFeatures(BlockGenerationData.RefinementData data) '''
		«FOR c : data.configurations»
			«IF c.workflow == RefinementWorkflow.GENERATE_STUB»
			
			«generateOOSEMType(OOSEMUtils.getTypeForNextPhase(c.refinedFeature.OOSEMBlockType))» «OOSEMGeneratorUtils.getSysMLType(c.refinedFeature.object as Type)» def «c.newTypeName» :> «c.refinedFeature.type.name» {
			    //TODO: Auto-generated block skeleton
			}
			«ENDIF»
		«ENDFOR»
	'''
}