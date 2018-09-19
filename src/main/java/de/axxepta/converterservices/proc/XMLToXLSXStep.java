package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

class XMLToXLSXStep extends Step {

    XMLToXLSXStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XML_XLSX;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String row = "Row";
        String column = "Cell";

        for (String line : parameters) {
            String[] components = line.split(" *= *");
            if (components[0].toLowerCase().equals("tr") || components[0].toLowerCase().equals("row")) {
                row = components[components.length - 1];
            }
            if (components[0].toLowerCase().equals("td") || components[0].toLowerCase().equals("column")) {
                column = components[components.length - 1];
            }
        }

        List<String> providedOutputNames = listifyOutput(pipe);
        List<String> usedOutputFiles = new ArrayList<>();
        int i = 0;
        for (String inFile : inputFiles) {
            String outputFile = getCurrentOutputFile(providedOutputNames, i, inFile, pipe);

            try (ByteArrayOutputStream os = ExcelUtils.XMLToExcel(inFile, row, column)) {
                IOUtils.byteArrayOutputStreamToFile(os, outputFile);
            }
            pipe.addGeneratedFile(outputFile);
            usedOutputFiles.add(outputFile);
            i++;
        }
        return usedOutputFiles;
    }

    private String getCurrentOutputFile(List<String> providedOutputNames, int current, String inputFile, Pipeline pipe) {
        return providedOutputNames.size() > current && !providedOutputNames.get(current).equals("") ?
                IOUtils.pathCombine(pipe.getWorkPath(), providedOutputNames.get(current)) :
                IOUtils.pathCombine(pipe.getWorkPath(),IOUtils.filenameFromPath(inputFile) + ".csv");
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return paramType.equals(Parameter.ADDITIONAL)|| paramType.equals(Parameter.PARAMS)|| assertStandardInput(param);
    }
}