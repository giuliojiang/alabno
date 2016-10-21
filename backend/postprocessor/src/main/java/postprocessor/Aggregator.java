package postprocessor;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.util.*;

public class Aggregator {

    private final List<String> jsonPaths;

    public Aggregator(List<String> jsonPaths) {
        this.jsonPaths = jsonPaths;
    }

    /**
     * Regroup annotations from all MicroServices' json output provided
     * by errorType inside new JSON file.
     * @return JSONArray with final, ordered output
     */
    public JSONArray aggregate() {

        Map<ErrorType, List<JSONObject>> map = new HashMap<>();

        // Create a list containing errors from all the MicroServices
        JSONObject jsonMicroServiceOutput;
        for (String path : jsonPaths) {
            jsonMicroServiceOutput = PostProcessorUtils.obtainJSONFile(path);
            JSONArray errors = (JSONArray) jsonMicroServiceOutput.get("errors");
            if (!errors.isEmpty()) {
                System.out.println("\nA MicroService failed to produce a valid output.\n" +
                        "Details on the errors:\n" + errors + "\n\n");
                // TODO: decide what to do in this case. For the moment, skipping output.
            } else {
                // Get array of annotations from MicroService's JSON output
                JSONArray annotations = (JSONArray) jsonMicroServiceOutput.get("annotations");
                addAnnotationToMapByErrorType(map, annotations);
            }
        }
        return generateJSONOutput(map);
    }

    /**
     * Regroup scores from all the provided JSON output of every MicroService
     * @return Map of MicroService names to their respective scores.
     */
    public Map<String, Double> getMicroServiceScores(List<String> jsonPaths) {

        Map<String, Double> microServiceToScoreMap = new HashMap<>();

        for (String path : jsonPaths) {
            JSONObject jsonMicroServiceOutput = PostProcessorUtils.obtainJSONFile(path);
            String currentFileName = new File(path).getName();
            String microServiceName = extractMicroServiceName(currentFileName);
            Double microServiceScore = (Double) jsonMicroServiceOutput.get("score");
            microServiceToScoreMap.put(microServiceName, microServiceScore);
        }

        return microServiceToScoreMap;
    }

    private String extractMicroServiceName(String currentFileName) {
        int stringDotIndex = currentFileName.lastIndexOf(".");
        String baseFileName = currentFileName.substring(0, stringDotIndex);
        int underScoreIndex = baseFileName.lastIndexOf("_output");
        return baseFileName.substring(0, underScoreIndex);
    }

    /**
     * Generate JSON formatted string containing
     * all annotations arranged by error type in
     * different JSON arrays
     * @param map The map containing all the micro-service annotations
     * @return Returns all the errors as a JSON string
     */
    // The JSON library does not cope well with generics, suppress unchecked warnings.
    @SuppressWarnings("unchecked")
    private JSONArray generateJSONOutput(Map<ErrorType, List<JSONObject>> map) {
        JSONArray finalOutput = new JSONArray();
        for (ErrorType type : ErrorType.values()) {
            List<JSONObject> associatedAnnotations = map.get(type);
            Map<String, List<JSONObject>> typeToError = new HashMap<>();
            typeToError.put(type.toString(), associatedAnnotations);
            if (associatedAnnotations != null) {
                finalOutput.add(typeToError);
            }
        }
        return finalOutput;
    }

    /**
     * Add annotations in map arranging by error type
     * @param map The map containing all the micro-service annotations grouped by error types
     * @param annotations The list with all the micro-service annotations
     */
    // The JSON library does not cope well with generics, suppress unchecked warnings.
    @SuppressWarnings("unchecked")
    private void addAnnotationToMapByErrorType(Map<ErrorType, List<JSONObject>> map, JSONArray annotations) {
        for (JSONObject annotation : (Iterable<JSONObject>) annotations) {
            String jsonErrorType = (String) annotation.get("errortype");
            ErrorType errorType = ErrorType.convertStringToErrorType(jsonErrorType);

            List<JSONObject> jsonListByErrorType = new ArrayList<>();
            if (map.containsKey(errorType)) {
                jsonListByErrorType = map.get(errorType);
            }
            jsonListByErrorType.add(annotation);
            map.put(errorType, jsonListByErrorType);
        }
    }
}
