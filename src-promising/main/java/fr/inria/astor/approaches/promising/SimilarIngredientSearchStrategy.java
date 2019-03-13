package fr.inria.astor.approaches.promising;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.apache.log4j.Logger;

import fr.inria.astor.approaches.jgenprog.operators.ReplaceOp;
import fr.inria.astor.core.entities.Ingredient;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.RandomManager;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.IngredientPool;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.scopes.ExpressionTypeIngredientSpace;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.ingredientSearch.RandomSelectionTransformedIngredientStrategy;
import fr.inria.astor.core.solutionsearch.spaces.operators.AstorOperator;
import fr.inria.astor.core.stats.Stats;
import fr.inria.astor.util.MapList;
import fr.inria.astor.util.StringUtil;

/**
 * A strategy to pick an ingredient from the fix space using code fragments'
 * lexical similarities.
 * 
 * @author Gabin An
 *
 */
public class SimilarIngredientSearchStrategy extends RandomSelectionTransformedIngredientStrategy {

	public SimilarIngredientSearchStrategy(IngredientPool space) {
		super(space);
	}

	@Override
	public Ingredient getFixIngredient(ModificationPoint modificationPoint, AstorOperator operationType) {

		int attemptsBaseIngredients = 0;

		log.info("****************GABIN INGREDIENTS**************");

		List<Ingredient> baseElements = getNotExhaustedBaseElements(modificationPoint, operationType);

		if (baseElements == null || baseElements.isEmpty()) {
			log.debug("Any template available for mp " + modificationPoint);
			List usedElements = this.exhaustTemplates.get(getKey(modificationPoint, operationType));
			if (usedElements != null)
				log.debug("#templates already used: " + usedElements.size());
			return null;
		}

		int elementsFromFixSpace = baseElements.size();
		log.debug("Templates availables: " + elementsFromFixSpace);

		Stats.currentStat.getIngredientsStats().addSize(Stats.currentStat.getIngredientsStats().ingredientSpaceSize,
				baseElements.size());

		while (attemptsBaseIngredients < elementsFromFixSpace) {

			log.debug(String.format("Attempts Base Ingredients  %d total %d", attemptsBaseIngredients,
					elementsFromFixSpace));

			Ingredient baseIngredient = getRandomStatementFromSpace(baseElements);

			if (baseIngredient == null || baseIngredient.getCode() == null) {

				return null;
			}

			Ingredient refinedIngredient = getNotUsedTransformedElement(modificationPoint, operationType,
					baseIngredient);

			attemptsBaseIngredients++;

			if (refinedIngredient != null) {

				refinedIngredient.setDerivedFrom(baseIngredient.getCode());
				return refinedIngredient;
			}

		} // End while

		log.debug("--- no mutation left to apply in element "
				+ StringUtil.trunc(modificationPoint.getCodeElement().getShortRepresentation())
				+ ", search space size: " + elementsFromFixSpace);
		return null;

	}

	@Override
	protected Ingredient getOneIngredientFromList(List<Ingredient> ingredientsAfterTransformation) {

		if (ingredientsAfterTransformation.isEmpty()) {
			log.debug("No more elements from the ingredients space");
			return null;
		}
		log.debug(String.format("Obtaining the best element out of %d: %s", ingredientsAfterTransformation.size(),
				ingredientsAfterTransformation.get(0).getCode()));
		// Return the first one
		return ingredientsAfterTransformation.get(0);
	}

	private Ingredient getTemplateByWeighted(List<Ingredient> elements, List<String> elements2String,
			Map<String, Double> probs) {
		// Random value
		Double randomElement = RandomManager.nextDouble();

		int i = 0;
		for (String template : probs.keySet()) {
			double probTemplate = probs.get(template);
			if (randomElement <= probTemplate) {
				int index = elements2String.indexOf(template);
				Ingredient templateElement = elements.get(index);
				log.debug("BI with prob "+probTemplate+" "+(i++) +" "+templateElement);
				return templateElement;
			}
		}
		return null;
	}

	@Override
	public List<Ingredient> getNotExhaustedBaseElements(ModificationPoint modificationPoint,
			AstorOperator operationType) {

		List<Ingredient> elements = this.ingredientSpace.getIngredients(modificationPoint.getCodeElement());

		if (elements == null)
			return null;

		if (operationType instanceof ReplaceOp) {
			//info -> debug
			log.info("ModificationPoint: " + modificationPoint.getCodeElement().toString());
			
			// Create JSON object
			JSONObject obj = new JSONObject();
			obj.put("ModificationPoint", modificationPoint.getCodeElement().toString());
			JSONArray ingredients = new JSONArray();
			for (Ingredient cm : elements) {
				ingredients.add(cm.toString());
			}
			obj.put("Ingredients", ingredients);

			String location = ConfigurationProperties.getProperty("location");
			String modelInputPath = location + "/.simInput" + Instant.now().toEpochMilli();

			try(FileWriter modelInput = new FileWriter(modelInputPath)) {
				modelInput.write(obj.toJSONString());
				log.info("Successfully Copied JSON object to File ...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<Ingredient> uniques = new ArrayList<>(elements);

		String key = getKey(modificationPoint, operationType);
		List<Ingredient> exhaustives = this.exhaustTemplates.get(key);

		if (exhaustives != null) {
			boolean removed = uniques.removeAll(exhaustives);
		}
		return uniques;
	}

}
