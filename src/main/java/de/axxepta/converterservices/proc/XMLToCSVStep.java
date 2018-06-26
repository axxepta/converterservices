package de.axxepta.converterservices.proc;

import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

class XMLToCSVStep extends Step {

    XMLToCSVStep(String name, Object input, Object output, Object additional, String... params) {
        super(name, input, output, additional, params);
    }

    @Override
    Pipeline.StepType getType() {
        return Pipeline.StepType.XML_CSV;
    }

    @Override
    Object execAction(final Pipeline pipe, final List<String> inputFiles, final String... parameters)
            throws Exception
    {
        String row = "tr";
        String column = "td";
        String delimiter = ";";
        for (String line : parameters) {
            String[] components = line.split(" *= *");
            if (components[0].toLowerCase().equals("tr") || components[0].toLowerCase().equals("row")) {
                row = components[components.length - 1];
            }
            if (components[0].toLowerCase().equals("td") || components[0].toLowerCase().equals("column")) {
                column = components[components.length - 1];
            }
            if (components[0].toLowerCase().startsWith("delim") || components[0].toLowerCase().startsWith("sep")) {
                delimiter = components[components.length - 1];
            }
        }

        List<String> outputFiles = new ArrayList<>();
        String outputFile;
        int i = 0;
        for (String inFile : inputFiles) {
            if ((output instanceof List) && (((List) output).size() == inputFiles.size())) {
                outputFile = IOUtils.pathCombine(pipe.getWorkPath(), (String) ((List) output).get(i));
            } else if (StringUtils.isNoStringOrEmpty(output)) {
                outputFile = IOUtils.filenameFromPath(inFile) + ".csv";
            } else {
                outputFile = IOUtils.pathCombine(pipe.getWorkPath(), (String) output);
            }

            try (ByteArrayOutputStream os = ExcelUtils.XMLToCSV(inFile, row, column, delimiter)) {
                IOUtils.ByteArrayOutputStreamToFile(os, outputFile);
            }
            pipe.addGeneratedFile(outputFile);
            outputFiles.add(outputFile);
            i++;
        }
        actualOutput = outputFiles;
        return outputFiles;
    }

    @Override
    protected boolean assertParameter(final Parameter paramType, final Object param) {
        return paramType.equals(Parameter.ADDITIONAL)|| paramType.equals(Parameter.PARAMS)|| assertStandardInput(param);
    }
}
