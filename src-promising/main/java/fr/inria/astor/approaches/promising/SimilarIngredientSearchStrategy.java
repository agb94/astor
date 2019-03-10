package fr.inria.astor.approaches.promising;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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

	List<String> elements2String = null;
	Map<String, Double> probs = null;

	@Override
	public List<Ingredient> getNotExhaustedBaseElements(ModificationPoint modificationPoint,
			AstorOperator operationType) {

		List<Ingredient> elements = super.getNotExhaustedBaseElements(modificationPoint, operationType);

		if(elements == null){
			return null;
		}

		//info -> debug
		log.info("ModificationPoint: " + modificationPoint.getCodeElement().toString());

		// Ingredients from space
		// ingredients to string
		int count = 0;
		elements2String = new ArrayList<>();
		for (Ingredient cm : elements) {
			elements2String.add(cm.toString());
			if (count < 10) {
				log.info("ingredient: " + cm.toString());
			}
			count += 1;
		}

		return elements;
	}

	@Override
	protected Ingredient getRandomStatementFromSpace(List<Ingredient> fixSpace) {

		if (ConfigurationProperties.getPropertyBool("frequenttemplate"))

			return getTemplateByWeighted(fixSpace, elements2String, probs);
		else
			return super.getRandomStatementFromSpace(fixSpace);
	}

}
