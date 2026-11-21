package dev.zyon;
import java.lang.reflect.Method;
import org.junit.Test;
import static org.junit.Assert.*;
public class FeatureHelpTest {
 @Test public void everyRegisteredFunctionHasItsOwnExplanation()throws Exception{Zyon client=new Zyon();Method register=Zyon.class.getDeclaredMethod("registerFeatures");register.setAccessible(true);register.invoke(client);assertEquals(17,client.features.size());for(Feature feature:client.features){assertTrue("No help for "+feature.id,FeatureHelp.contains(feature.id));assertTrue(FeatureHelp.describe(feature).length()>feature.description.length());}assertEquals("Azul neon",client.feature("theme").setting("accent").display());assertEquals(0xFF249BFF,client.accent());}
 @Test public void descriptionsDoNotChangeFeatureStateOrSettings(){Feature feature=new Feature("crosshair","Mira","short",Feature.Category.RENDER,Setting.number("range","Alcance",3,1,6,.1));feature.key=19;String description=FeatureHelp.describe(feature);assertTrue(description.contains("somente visual"));assertTrue(description.contains("nao altera alcance"));assertFalse(feature.enabled());assertEquals(19,feature.key);assertEquals(3,feature.number("range"),0);}
}
