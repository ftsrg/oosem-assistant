package hu.bme.mit.sysml.oosem.generators

import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.BlockConfiguration
import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.NameMapping
import hu.bme.mit.sysml.oosem.generators.BlockConfigurationData.ConfigurationStructure.FeatureConfiguration
import java.util.Set
import org.eclipse.emf.ecore.EObject
import org.omg.sysml.lang.sysml.AttributeUsage
import org.omg.sysml.lang.sysml.Expression
import org.omg.sysml.lang.sysml.FeatureValue
import org.omg.sysml.lang.sysml.LiteralBoolean
import org.omg.sysml.lang.sysml.LiteralInteger
import org.omg.sysml.lang.sysml.LiteralRational
import org.omg.sysml.lang.sysml.LiteralString
import org.omg.sysml.lang.sysml.Type
import org.omg.sysml.lang.sysml.Element

class UVLgenerator {
	def String generate(BlockConfigurationData data) '''
	features
		«generateBlock(data.blockConfigurationRoot, data.nameMapping)»
	'''
	
	def String generateBlock(BlockConfiguration root, NameMapping nameMapping) '''
		«nameMapping.mapElementToId(root)»«generateAttributes(root.object.object)»
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
		«nameMapping.mapElementToId(ms)»«generateAttributes(ms.object.object)»
			alternative
				«FOR alt : ms.alternatives»
				«generateBlock(alt, nameMapping)»
				«ENDFOR»
		«ENDFOR»
	'''
	
	def String generateAttributes(EObject element) {
		if (!(element instanceof Type)) {
			return ""
		}
		val attributes = (element as Type).ownedFeature
			.filter(AttributeUsage)
			.map[generateAttribute]
			.filterNull
			.toList
		if (attributes.isEmpty) {
			return ""
		}
		return " {" + attributes.join(", ") + "}"
	}
	
	def private String generateAttribute(AttributeUsage attribute) {
		val name = uvlAttributeName(attribute)
		if (name === null) {
			return null
		}
		
		val value = attribute.ownedMembership.filter(FeatureValue).head?.value
		val literal = toUvlLiteral(value, attribute.owner)
		
		return if (literal === null) name else name + " " + literal // Valueless attributes as backup
	}
	
	def private String toUvlLiteral(Expression expression, Element context) {
		if (expression === null) {
			return null
		}
		
		// Plain literals: no evaluation
		val direct = expression.literalToUvl
		if (direct !== null) {
			return direct
		}
		
		// Anything else: model-level evaluator
		val result = try {
			expression.evaluate(context)
		} catch (Exception e) {
			null
		}
		if (result === null || result.size != 1) {
			return null
		}
		return switch v : result.head {
			Expression: v.literalToUvl
			default: null
		}
	}
	
	def private String literalToUvl(Expression expression) {
		switch expression {
			LiteralInteger: expression.value.toString
			LiteralRational: expression.value.toString
			LiteralBoolean: String.valueOf(expression.isValue)
			LiteralString: "'" + expression.value.replace("'", "\\'").replace("\n", " ") + "'"
			default: null
		}
	}
	
	def private String uvlAttributeName(AttributeUsage attribute) {
		val raw = attribute.declaredName ?: attribute.shortName
		if (raw.isNullOrEmpty) {
			return null
		}
		val sanitized = raw.replaceAll("\\W", "_")
		return if (Character.isDigit(sanitized.charAt(0))) "_" + sanitized else sanitized
	}
}