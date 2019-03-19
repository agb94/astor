package fr.inria.astor.approaches.promising;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Arrays;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import org.apache.log4j.Logger;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import fr.inria.astor.approaches.jgenprog.operators.ReplaceOp;
import fr.inria.astor.core.entities.Ingredient;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.RandomManager;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.IngredientPool;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.IngredientSearchStrategy;
import fr.inria.astor.core.solutionsearch.spaces.ingredients.scopes.ExpressionTypeIngredientSpace;
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
public class SimilarIngredientSearchStrategy extends IngredientSearchStrategy {


	private static final Boolean DESACTIVATE_CACHE = ConfigurationProperties
			.getPropertyBool("desactivateingredientcache");

	protected Logger log = Logger.getLogger(this.getClass().getName());

	public SimilarIngredientSearchStrategy(IngredientPool space) {
		super(space);

	}

	public MapList<String, String> cache = new MapList<>();

	/**
	 * Return an ingredient. As it has a cache, it never returns twice the same
	 * ingredient.
	 * 
	 * @param modificationPoint
	 * @param targetStmt
	 * @param operationType
	 * @param elementsFromFixSpace
	 * @return
	 */
	@Override
	public Ingredient getFixIngredient(ModificationPoint modificationPoint, AstorOperator operationType) {

		int attemptsBaseIngredients = 0;

		List<Ingredient> baseElements = getIngredientsFromSpace(modificationPoint, operationType);

		if (baseElements == null || baseElements.isEmpty()) {
			log.debug("Any element available for mp " + modificationPoint);
			return null;
		}

		int elementsFromFixSpace = baseElements.size();
		log.debug("Templates availables" + elementsFromFixSpace);

		Stats.currentStat.getIngredientsStats().addSize(Stats.currentStat.getIngredientsStats().ingredientSpaceSize,
				baseElements.size());

		while (attemptsBaseIngredients < elementsFromFixSpace) {

			attemptsBaseIngredients++;
			log.debug(String.format("Attempts Base Ingredients  %d total %d", attemptsBaseIngredients,
					elementsFromFixSpace));

			Ingredient baseIngredient = getRandomStatementFromSpace(baseElements);

			String newingredientkey = getKey(modificationPoint, operationType);

			if (baseIngredient != null && baseIngredient.getCode() != null) {

				// check if the element was already used
				if (DESACTIVATE_CACHE || !this.cache.containsKey(newingredientkey)
						|| !this.cache.get(newingredientkey).contains(baseIngredient.getChacheCodeString())) {
					this.cache.add(newingredientkey, baseIngredient.getChacheCodeString());
					return baseIngredient;
				}

			}

		} // End while

		log.debug("--- no mutation left to apply in element "
				+ StringUtil.trunc(modificationPoint.getCodeElement().getShortRepresentation())
				+ ", search space size: " + elementsFromFixSpace);
		return null;

	}

	public String getKey(ModificationPoint modPoint, AstorOperator operator) {
		String lockey = modPoint.getCodeElement().getPosition().toString() + "-" + modPoint.getCodeElement() + "-"
				+ operator.toString();
		return lockey;
	}

	/**
	 * 
	 * @param fixSpace
	 * @return
	 */
	protected Ingredient getRandomStatementFromSpace(List<Ingredient> fixSpace) {
		if (fixSpace == null)
			return null;
		int size = fixSpace.size();
		int index = RandomManager.nextInt(size);
		return fixSpace.get(index);

	}

	public List<Ingredient> getIngredientsFromSpace(ModificationPoint modificationPoint, AstorOperator operationType) {
		List<Ingredient> elements = this.ingredientSpace.getIngredients(modificationPoint.getCodeElement());

		if (elements == null)
			return null;

		if (operationType instanceof ReplaceOp) {
			//info -> debug
			//log.info("ModificationPoint: " + modificationPoint.getCodeElement().toString());
			
			// Create JSON object
			JSONObject obj = new JSONObject();
			obj.put("ModificationPoint", modificationPoint.getCodeElement().toString());
			JSONArray ingredients = new JSONArray();
			for (Ingredient cm : elements) {
				ingredients.add(cm.toString());
			}
			obj.put("Ingredients", ingredients);

			String location = ConfigurationProperties.getProperty("location");
			long timestamp = Instant.now().toEpochMilli();
			String modelInputPath = location + "/.simInput" + timestamp;
			String modelOutputPath = location + "/.simOutput" + timestamp;

			try(FileWriter modelInput = new FileWriter(modelInputPath)) {
				modelInput.write(obj.toJSONString());
			} catch (IOException e) {
				e.printStackTrace();
			}

			DefaultExecutor executor = new DefaultExecutor();
			String[] command = { "python", "src-promising/main/python/find_similar.py", modelInputPath };
			CommandLine cmdLine = CommandLine.parse(command[0]);
		    for (int i = 1, n = command.length; i < n; i++ ) {
		        cmdLine.addArgument(command[i]);
		    }

		    try {
		    	executor.execute(cmdLine);
			} catch (IOException e) {
				e.printStackTrace();
			}

		    List<Integer> filteringResult = new ArrayList<Integer>();
		    try {
	            File file = new File(modelOutputPath);
	            Scanner scan = new Scanner(file);
	            if (scan.hasNextLine()){
	            	for (String b : scan.nextLine().split(","))	{
	            		filteringResult.add(Integer.parseInt(b));
		    		}
	            }
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        }

	        List<Ingredient> filteredElements = new ArrayList<Ingredient>();
	        for (int i = 0, n = elements.size(); i < n; i++) {
	        	if (filteringResult.get(i) == 1){
	        		filteredElements.add(elements.get(i));
	        	}
	        }
	        log.info("Ingredients Filtering: " + elements.size() + " -> " + filteredElements.size());
	        elements = filteredElements;
		}

		return elements;
		/*
		List<Ingredient> uniques = new ArrayList<>(elements);

		String key = getKey(modificationPoint, operationType);
		List<Ingredient> exhaustives = this.exhaustTemplates.get(key);

		if (exhaustives != null) {
			boolean removed = uniques.removeAll(exhaustives);
		}
		return uniques;
		*/
	}

}
