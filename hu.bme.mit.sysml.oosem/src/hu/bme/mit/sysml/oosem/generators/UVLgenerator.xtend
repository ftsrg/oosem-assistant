package hu.bme.mit.sysml.oosem.generators

import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.BlockConfiguration
import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.NameMapping
import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.FeatureConfiguration
import java.util.Set

class UVLgenerator {
	def String generate(BlockConfigurationData data) '''
	features
		«generateBlock(data.blockConfigurationRoot, data.nameMapping)»
	'''
	
	def String generateBlock(BlockConfiguration root, NameMapping nameMapping) '''
		«nameMapping.mapElementToId(root)»
			«IF !root.mandatorySubsystems.isEmpty»
			mandatory
				«generateSubsystems(root.mandatorySubsystems, nameMapping)»
			«ENDIF»
			«IF !root.optionalSubsystems.isEmpty»
			optional
				«generateSubsystems(root.optionalSubsystems, nameMapping)»
			«ENDIF»
	'''
	
	def String generateSubsystems(Set<FeatureConfiguration> subsystems, NameMapping nameMapping) '''
		«FOR ms : subsystems»
		«nameMapping.mapElementToId(ms)»
			alternative
				«FOR alt : ms.alternatives»
				«generateBlock(alt, nameMapping)»
				«ENDFOR»
		«ENDFOR»
	'''
}