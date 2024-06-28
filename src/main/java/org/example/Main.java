package org.example;

import com.opencsv.CSVReader;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.TargetField;
import org.jpmml.transpiler.FileTranspiler;
import org.jpmml.transpiler.Transpiler;
import org.jpmml.transpiler.TranspilerTransformer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        try {

            // Specify the path to the PMML file
            File pmmlFile = new File("src/main/resources/model_xgb_final.pmml");
            if (!pmmlFile.exists()) {
                throw new IOException("PMML file not found: " + pmmlFile.getAbsolutePath());
            }

            // Create a loader and load the PMML model
            LoadingModelEvaluatorBuilder evaluatorBuilder = new LoadingModelEvaluatorBuilder().load(pmmlFile);

//             Use Transpiler for code conversion
            Transpiler transpiler = new FileTranspiler("org.example", new File(pmmlFile.getAbsolutePath() + ".jar"));
            evaluatorBuilder = evaluatorBuilder.transform(new TranspilerTransformer(transpiler));

            // Build the model evaluator
            Evaluator evaluator = evaluatorBuilder.build();

            // Verify the model
//            evaluator.verify();
//            System.out.println("PMML model successfully loaded and verified.");

            // Read the CSV file
            try (CSVReader reader = new CSVReader(new FileReader("src/main/resources/X_test_verify.csv"))) {
                List<String[]> lines = reader.readAll();
                String[] headers = lines.get(0);

                List<Map<String, String>> allData = new ArrayList<>();

                // Parse CSV data into a list of maps
                for (int i = 1; i < lines.size(); i++) {
                    String[] line = lines.get(i);
                    Map<String, String> rowData = new LinkedHashMap<>();

                    for (int j = 0; j < headers.length; j++) {
                        rowData.put(headers[j], line[j]);
                    }

                    allData.add(rowData);
                }

                // Iterate over all rows and make predictions
                for (Map<String, String> data : allData) {
//                    System.out.println("Input data: " + data); // Debug statement to print input data

                    Map<String, FieldValue> arguments = new LinkedHashMap<>();

                    // Prepare input fields
                    for (InputField inputField : evaluator.getInputFields()) {
                        String inputName = inputField.getName().toString();
                        Object rawValue = data.get(inputName);
//                        System.out.println("rawValue: " + rawValue+"inputName: " + inputName); // Debug statement to print input data
                        FieldValue inputValue = inputField.prepare(rawValue);
                        arguments.put(inputName, inputValue);
                    }

                    // Evaluate the model
                    Map<String, ?> results = evaluator.evaluate(arguments);

                    // Output the prediction results
                    List<TargetField> targetFields = evaluator.getTargetFields();
                    for (TargetField targetField : targetFields) {
                        String targetName = targetField.getName().toString();
                        Object targetValue = results.get(targetName);
                        System.out.println("Predicted value for " + targetName + ": " + targetValue);
                    }
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}