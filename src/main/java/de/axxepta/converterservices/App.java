package de.axxepta.converterservices;

import de.axxepta.converterservices.proc.PipeExec;
import de.axxepta.converterservices.tools.CmdUtils;
import de.axxepta.converterservices.tools.ExcelUtils;
import de.axxepta.converterservices.tools.ImageUtils;
import de.axxepta.converterservices.tools.PDFUtils;
import de.axxepta.converterservices.utils.IOUtils;
import de.axxepta.converterservices.utils.ServletUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

import static spark.Spark.*;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static final List<String> activeDirectories = new ArrayList<>();

    public static final String STATIC_FILE_PATH = System.getProperty("user.home") + "/.converterservices";
    public static final String TEMP_FILE_PATH = STATIC_FILE_PATH + "/temp";

    private static final String PATH_HELLO              = "/hello";
    private static final String PATH_STOP               = "/stop";
    private static final String PATH_THUMB              = "/image/singlethumbnail";
    private static final String PATH_THUMBS             = "/image/thumbnail";
    private static final String PATH_META               = "/file/metadata";
    private static final String PATH_SPLIT              = "/pdf/split";
    private static final String PATH_EXCEL              = "/excel";
    private static final String PATH_PIPELINE           = "/pipeline";
    private static final String PATH_DOWNLOAD           = "/static";
    private static final String PATH_UPLOAD             = "/upload";
    private static final String PATH_UPLOAD_THUMB       = "/form/thumb";
    private static final String PATH_UPLOAD_THUMBS      = "/form/thumbs";
    private static final String PATH_UPLOAD_META        = "/form/meta";
    private static final String PATH_UPLOAD_SPLIT       = "/form/pdfsplit";
    private static final String PATH_UPLOAD_EXCEL       = "/form/excel";

    private static final String PARAM_NAME              = ":name";
    private static final String PARAM_COMPACT           = "compact";
    private static final String PARAM_AS                = "as";
    private static final String PARAM_CUSTOM_XML        = "customXMLMapping";
    private static final String PARAM_SHEET_NAME        = "sheetName";
    private static final String PARAM_ATT_SHEET_NAME    = "attSheetName";
    private static final String PARAM_INDENT            = "indent";
    private static final String PARAM_FIRST_ROW_NAME    = "firstRowName";
    private static final String PARAM_FIRST_COL_ID      = "firstColId";
    private static final String PARAM_COLUMN_FIRST      = "columnFirst";
    private static final String PARAM_SHEET_TAG         = "sheet";
    private static final String PARAM_ROW_TAG           = "row";
    private static final String PARAM_COLUMN_TAG        = "column";
    private static final String PARAM_SEPARATOR         = "separator";
    private static final String PARAM_PWD               = "pwd";
    private static final String PARAM_RESPONSE          = "response";
    private static final String PARAM_VAL_SCALE         = "scale";
    private static final String PARAM_VAL_CROP          = "crop";
    private static final String PARAM_VAL_PDF           = "pdf";
    private static final String PARAM_VAL_PNG           = "png";
    private static final String PARAM_VAL_SINGLE        = "single";
    private static final String PARAM_VAL_MULTI         = "multi";
    private static final String PARAM_CLEANUP           = "cleanup";

    private static final String HELLO_PAGE              = "static/hello.html";
    private static final String THUMB_UPLOAD_FORM       = "static/form_thumb.html";
    private static final String THUMBS_UPLOAD_FORM      = "static/form_thumbs.html";
    private static final String META_UPLOAD_FORM        = "static/form_meta.html";
    private static final String PDFSPLIT_UPLOAD_FORM    = "static/form_pdfsplit.html";
    private static final String EXCEL_UPLOAD_FORM       = "static/form_excel.html";

    public static final String TYPE_JPEG               = "image/jpeg";
    public static final String TYPE_PNG                = "image/png";
    public static final String TYPE_PDF                = "application/pdf";
    public static final String TYPE_XLSX               = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String TYPE_CSV                = "text/csv";
    public static final String TYPE_XML                = "application/xml";
    public static final String TYPE_OCTET              = "application/octet-stream";

    private static final String FILE_PART               = "FILE";
    private static final String AS_PNG                  = "AS_PNG";
    private static final String MULTIPART_FORM_DATA     = "multipart/form-data";
    private static final String HTML_OPEN               = "<html><body><h1>";
    private static final String HTML_CLOSE              = "</h1></body></html>";
    private static final String NO_SUCH_FILE            = "<html><body><h1>File does not exist.</h1></body></html>";
    private static final String NO_FILES_JSON           = "{\"ERROR\": \"No temporary file found.\"}";
    private static final String EXCEPTION_OPEN_JSON     = "{\"EXCEPTION\": \"";
    private static final String EXCEPTION_CLOSE_JSON    = "\"}";

    private static String pwd = "";


    public static void main(String[] args) {

        if (args.length > 0)
            pwd = args[0];

        init("");

    }

//########################################################

    static void init(String basePath) {

        try {
            de.axxepta.converterservices.utils.IOUtils.safeCreateDirectory(TEMP_FILE_PATH);
        } catch (IOException ie) {
            LOGGER.error("Couldn't create directory for temporary files!");
        }

        get(basePath + PATH_HELLO, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(HELLO_PAGE),
                        basePath + PATH_THUMB, basePath + PATH_THUMBS,
                        basePath + PATH_META, basePath + PATH_SPLIT, basePath + PATH_UPLOAD_THUMB,
                        basePath + PATH_UPLOAD_THUMBS, basePath + PATH_UPLOAD_META,
                        basePath + PATH_UPLOAD_SPLIT, basePath + PATH_UPLOAD_EXCEL, basePath + PATH_STOP)
        );

        get(basePath + PATH_DOWNLOAD + "/" + PARAM_NAME, (request, response) -> {
            HttpServletResponse raw = ServletUtils.singleFileResponse(response, request.params(PARAM_NAME));
            File file = new File(STATIC_FILE_PATH + "/" + request.params(PARAM_NAME));
            if (file.exists()) {
                try (InputStream is = new FileInputStream(file)) {
                    de.axxepta.converterservices.utils.IOUtils.copyStreams(is, raw.getOutputStream());
                    raw.getOutputStream().close();
                } catch (Exception e) {
                    return HTML_OPEN + e.getMessage() + HTML_CLOSE;
                }
            } else {
                return NO_SUCH_FILE;
            }
            return raw;
        });

        get(basePath + PATH_UPLOAD_THUMB, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(THUMB_UPLOAD_FORM),
                        basePath + PATH_THUMB, FILE_PART)
        );

        get(basePath + PATH_UPLOAD_THUMBS, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(THUMBS_UPLOAD_FORM),
                        basePath + PATH_THUMBS, FILE_PART)
        );

        get(basePath + PATH_UPLOAD_META, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(META_UPLOAD_FORM),
                        basePath + PATH_META, PARAM_COMPACT, FILE_PART)
        );

        get(basePath + PATH_UPLOAD_SPLIT, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(PDFSPLIT_UPLOAD_FORM),
                        PARAM_VAL_PNG, PARAM_VAL_PDF, basePath + PATH_SPLIT, PARAM_AS, FILE_PART, AS_PNG)
        );

        get(basePath + PATH_UPLOAD_EXCEL, (request, response) ->
                String.format(de.axxepta.converterservices.utils.IOUtils.getResourceAsString(EXCEL_UPLOAD_FORM),
                        basePath + PATH_EXCEL, PARAM_AS, PARAM_CUSTOM_XML, PARAM_INDENT, PARAM_FIRST_ROW_NAME, PARAM_FIRST_COL_ID,
                        PARAM_COLUMN_FIRST, PARAM_SHEET_TAG, PARAM_ROW_TAG, PARAM_COLUMN_TAG, PARAM_SHEET_NAME,
                        PARAM_ATT_SHEET_NAME, PARAM_SEPARATOR, FILE_PART)
        );


        post(basePath + PATH_THUMB + "/*/*", MULTIPART_FORM_DATA, App::singleImageHandling);

        post(basePath + PATH_THUMB + "/*", MULTIPART_FORM_DATA, App::singleImageHandling);

        post(basePath + PATH_THUMBS + "/*/*", MULTIPART_FORM_DATA, App::imageHandling);

        post(basePath + PATH_THUMBS + "/*", MULTIPART_FORM_DATA, App::imageHandling);

        post(basePath + PATH_UPLOAD, MULTIPART_FORM_DATA, (request, response) -> {
            MultipartConfigElement multipartConfigElement = new MultipartConfigElement(STATIC_FILE_PATH);
            request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

            Collection<Part> parts = null;
            try {
                parts = request.raw().getParts();
            } catch (IOException | ServletException ex) {
                ex.printStackTrace();
            }
            StringBuilder builder = new StringBuilder("<html><body>");
            if (parts != null) {
                for (Part part : parts) {
                    builder.append("<p>Name: ").append(part.getName()).append("<br>");
                    builder.append("Size: ").append(part.getSize()).append("<br>");
                    builder.append("Filename: ").append(part.getSubmittedFileName()).append("<br></p>");
                    part.write(part.getSubmittedFileName());
                }
            }
            return builder.append("</body></html>").toString();
        });

        post(basePath + PATH_META, (request, response) -> {
            boolean compact = ServletUtils.checkQueryParameter(request, PARAM_COMPACT, false, "true", true);
            List<String> files = ServletUtils.parseMultipartRequest(request, FILE_PART, new ArrayList<>());
            try {
                if (files.size() > 0) {
                    try (ByteArrayOutputStream out = CmdUtils.exif(compact, "-json", TEMP_FILE_PATH + "/" + files.get(0))) {
                        cleanTemp(files);
                        return new String(out.toByteArray(), "UTF-8");
                    }
                } else return NO_FILES_JSON;
            } catch (Exception e) {
                e.printStackTrace();
                return EXCEPTION_OPEN_JSON + e.getMessage() + EXCEPTION_CLOSE_JSON;
            }
        });

        post(basePath + PATH_SPLIT, MULTIPART_FORM_DATA, (request, response) -> {
            boolean as_png = ServletUtils.checkQueryParameter(request, PARAM_AS, false, PARAM_VAL_PNG, false);
            List<String> files;
            List<String> outputFiles;
            try {
                List<String> partNames = new ArrayList<>();
                files = ServletUtils.parseMultipartRequest(request, FILE_PART, partNames);
                /*if (partNames.contains(AS_PNG))
                    as_png = true;*/
                outputFiles = PDFUtils.splitPDF(files.get(0), as_png, TEMP_FILE_PATH);
            } catch (IOException | InterruptedException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            }

            if (outputFiles.size() > 0) {
                try {
                    HttpServletResponse raw = ServletUtils.multiPartResponse(response);
                    for (String fileName : outputFiles) {
                        File file = new File(TEMP_FILE_PATH + "/" + fileName);
                        if (file.exists()) {
                            files.add(fileName);
                            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                                ServletUtils.addMultiPartFile(raw.getOutputStream(), as_png ? TYPE_PNG : TYPE_PDF, is, fileName);
                            }
                        }
                    }
                    ServletUtils.multiPartClose(raw.getOutputStream());
                    return raw;
                } catch (IOException ex) {
                    return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
                } finally {
                    cleanTemp(files);
                }
            } else {
                return NO_SUCH_FILE;
            }
        });

        post(basePath + PATH_EXCEL, App::excelHandling);

        post(basePath + PATH_PIPELINE, TYPE_XML, App::pipelineHandling);

        get(basePath + PATH_STOP, (request, response) -> {
            if (ServletUtils.getQueryParameter(request, PARAM_PWD).equals(pwd)) {
                stop();
                return "Services stopped.";
            } else {
                return "Wrong password provided for stopping server.";
            }

        });

    }


    private static Object pipelineHandling(Request request, Response response) {
        boolean cleanup = false;
        try {
            String pipelineString = request.body();
            Map<String, String> parameters = request.params();
            if (parameters.containsKey(PARAM_CLEANUP)) {
                if (parameters.get(PARAM_CLEANUP).equals("true"))
                    cleanup = true;
            }
            Object result = PipeExec.execProcessString(pipelineString);
            if (result instanceof Integer && result.equals(-1)) {
                response.status(500);
                return HTML_OPEN + "Error during pipeline execution" + HTML_CLOSE;
            } else if (result instanceof String) {
                if (IOUtils.isFile((String) result)) {
                    return ServletUtils.buildSingleFileResponse(response, (String) result);
                } else {
                    return HTML_OPEN + result + HTML_CLOSE;
                }
            } else if (result instanceof List && ((List)result).get(0) instanceof String) {
                List<String> results = (List)result;
                if (IOUtils.isFile(results.get(0))) {
                    HttpServletResponse raw = ServletUtils.multiPartResponse(response);
                    for (String fileName : results) {
                        if (IOUtils.isFile(fileName)) {
                            try (InputStream is = new BufferedInputStream(new FileInputStream(fileName))) {
                                String outputType = IOUtils.contentTypeByFileName(fileName);
                                ServletUtils.addMultiPartFile(raw.getOutputStream(), outputType, is, fileName);
                            }
                        }
                    }
                    ServletUtils.multiPartClose(raw.getOutputStream());
                    return raw;
                } else {
                    StringBuilder responseBuilder = new StringBuilder(HTML_OPEN);
                    for (String fileName : results) {
                        responseBuilder = responseBuilder.append("<div>").append(fileName).append("</div>");
                    }
                    return responseBuilder.append(HTML_CLOSE).toString();
                }
            } else {
                return HTML_OPEN + result + HTML_CLOSE;
            }
        } catch (Exception ex) {
            response.status(500);
            return HTML_OPEN + "Error during pipeline execution" + HTML_CLOSE;
        } finally {
            if (cleanup) {
                // ToDo:
            }
        }
    }

    private static Object excelHandling(Request request, Response response) {
        String sheetName = ServletUtils.getQueryParameter(request, PARAM_SHEET_NAME, "");
        String attSheetName = ServletUtils.getQueryParameter(request, PARAM_ATT_SHEET_NAME, "");
        String as = ServletUtils.getQueryParameter(request, PARAM_AS);
        String responseType = ServletUtils.getQueryParameter(request, PARAM_RESPONSE, PARAM_VAL_MULTI);
        boolean customXMLMapping = ServletUtils.checkQueryParameter(request, PARAM_CUSTOM_XML, false, "true", false);
        boolean indent = !ServletUtils.checkQueryParameter(request, PARAM_INDENT, false, "false", false);
        boolean firstRowName = !ServletUtils.checkQueryParameter(request, PARAM_FIRST_ROW_NAME, false, "false", false);
        boolean firstColumnId = ServletUtils.checkQueryParameter(request, PARAM_FIRST_COL_ID, false, "true", false);
        boolean columnFirst = ServletUtils.checkQueryParameter(request, PARAM_COLUMN_FIRST, false, "true", false);
        String sheet = ServletUtils.getQueryParameter(request, PARAM_SHEET_TAG, ExcelUtils.SHEET_EL);
        String row = ServletUtils.getQueryParameter(request, PARAM_ROW_TAG, ExcelUtils.ROW_EL);
        String column = ServletUtils.getQueryParameter(request, PARAM_COLUMN_TAG, ExcelUtils.COL_EL);
        String separator = ServletUtils.getQueryParameter(request, PARAM_SEPARATOR, ExcelUtils.DEF_SEPARATOR);

        List<String> files = ServletUtils.parseMultipartRequest(request, FILE_PART, new ArrayList<>());
        List<String> convertedFiles = new ArrayList<>();
        for (String file : files) {
            if (de.axxepta.converterservices.utils.IOUtils.isXLSX(file)) {
                convertedFiles.addAll(ExcelUtils.fromExcel(file,
                        as.toLowerCase().equals("xml") ? ExcelUtils.FileType.XML : ExcelUtils.FileType.CSV,
                        customXMLMapping, sheetName, separator, indent, columnFirst,
                        firstRowName, firstColumnId, "", sheet, row, column, attSheetName));
            }
            if (de.axxepta.converterservices.utils.IOUtils.isCSV(file)) {
                convertedFiles.add(ExcelUtils.CSVToExcel(file,
                        (sheetName.equals("")) ? ExcelUtils.DEF_SHEET_NAME : sheetName, separator));
            }
            if (de.axxepta.converterservices.utils.IOUtils.isXML(file)) {
                convertedFiles.add(ExcelUtils.XMLToExcel(file));
            }
        }

        if (files.size() > 0) {
            try {
                HttpServletResponse raw = responseType.toLowerCase().equals(PARAM_VAL_SINGLE) ?
                        ServletUtils.singleFileResponse(response, convertedFiles.get(0)) :
                        ServletUtils.multiPartResponse(response);
                int fileCounter = 0;
                for (String fileName : convertedFiles) {
                    File file = new File(TEMP_FILE_PATH + "/" + fileName);
                    if (file.exists()) {
                        files.add(fileName);
                        if (responseType.toLowerCase().equals(PARAM_VAL_SINGLE)) {
                            if (fileCounter == 0) {
                                try (InputStream is = new FileInputStream(file)) {
                                    de.axxepta.converterservices.utils.IOUtils.copyStreams(is, raw.getOutputStream());
                                    raw.getOutputStream().close();
                                }
                            }
                        } else {
                            String outputType;
                            if (de.axxepta.converterservices.utils.IOUtils.isXLSX(fileName))
                                outputType = TYPE_XLSX;
                            else if (de.axxepta.converterservices.utils.IOUtils.isXML(fileName))
                                outputType = TYPE_XML;
                            else
                                outputType = TYPE_CSV;
                            try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                                ServletUtils.addMultiPartFile(raw.getOutputStream(), outputType, is, fileName);
                            }
                        }
                        fileCounter++;
                    }
                }
                if (responseType.toLowerCase().equals(PARAM_VAL_MULTI.toLowerCase()))
                    ServletUtils.multiPartClose(raw.getOutputStream());
                return raw;
            } catch (IOException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            } finally {
                cleanTemp(files);
            }
        } else {
            return NO_SUCH_FILE;
        }
    }


    private static Object singleImageHandling(Request request, Response response) {
        List<String> files;
        try {
            files = thumbifyFiles(request);
        } catch (IOException | InterruptedException ex) {
            return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
        }

        if (files.size() > 0) {
            HttpServletResponse raw = ServletUtils.singleFileResponse(response, "thumb_" + ImageUtils.jpgFilename(files.get(0)));
            File file = new File(TEMP_FILE_PATH + "/" + ImageUtils.jpgFilename(files.get(0)));
            if (file.exists()) {
                files.add(ImageUtils.jpgFilename(files.get(0)));
                try (InputStream is = new FileInputStream(file)) {
                    de.axxepta.converterservices.utils.IOUtils.copyStreams(is, raw.getOutputStream());
                    raw.getOutputStream().close();
                    return raw;
                } catch (Exception e) {
                    return HTML_OPEN + e.getMessage() + HTML_CLOSE;
                } finally {
                    cleanTemp(files);
                }
            } else {
                cleanTemp(files);
                return NO_SUCH_FILE;
            }
        } else {
            return NO_SUCH_FILE;
        }
    }

    private static Object imageHandling(Request request, Response response) {
        List<String> files;
        try {
            files = thumbifyFiles(request);
        } catch (IOException | InterruptedException ex) {
            return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
        }

        if (files.size() > 0) {
            try {
                List<String> transformedFiles = new ArrayList<>();
                HttpServletResponse raw = ServletUtils.multiPartResponse(response);
                for (String fileName : files) {
                    File file = new File(TEMP_FILE_PATH + "/" + ImageUtils.jpgFilename(fileName));
                    if (file.exists()) {
                        transformedFiles.add(ImageUtils.jpgFilename(fileName));
                        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
                            ServletUtils.addMultiPartFile(raw.getOutputStream(), TYPE_JPEG, is, ImageUtils.jpgFilename(fileName));
                        }
                    }
                }
                files.addAll(transformedFiles);
                ServletUtils.multiPartClose(raw.getOutputStream());
                return raw;
            } catch (IOException ex) {
                return HTML_OPEN + ex.getMessage() + HTML_CLOSE;
            } finally {
                cleanTemp(files);
            }
        } else {
            return NO_SUCH_FILE;
        }
    }

    private static List<String> thumbifyFiles(Request request)
            throws IOException, InterruptedException {
        List<String> files = ServletUtils.parseMultipartRequest(request, FILE_PART, new ArrayList<>());
        String size = request.splat()[0];
        String fit = "";
        if (request.splat().length > 1)
            fit = request.splat()[1];
        String scaling;
        switch (fit.toLowerCase()) {
            case PARAM_VAL_CROP: {
                scaling = size + "^"; break;    // scale to shorter side, eventually cropped
            }
            case PARAM_VAL_SCALE: {
                scaling = size + "!"; break;    // scale to values, aspect ratio ignored
            }
            default: scaling = size;            // scale to fit, eventually borders
        }
        try {
            for (String file : files) {
                ImageUtils.thumbify(scaling, size, TEMP_FILE_PATH + "/" + file);
            }
        } catch (IOException | InterruptedException ex) {
            cleanTemp(files);
            throw ex;
        }
        return files;
    }

    private static void cleanTemp(List<String> files) {
        for (String file : files) {
            try {
                Files.delete(Paths.get(TEMP_FILE_PATH + "/" + file));
            } catch (IOException ex) {
                if (LOGGER != null) LOGGER.error(ex.getMessage());
            }
        }
    }


    public static String setTemPath() {
        String dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        synchronized (activeDirectories) {
            if (activeDirectories.contains(dateString)) {
                dateString += "__" + Integer.toString(activeDirectories.size());
            }
            activeDirectories.add(dateString);
        }
        try {
            IOUtils.safeCreateDirectory(IOUtils.pathCombine(App.TEMP_FILE_PATH, dateString));
        } catch (IOException ex) {
            return "";
        }
        return dateString;
    }

    private static void cleanup(String dateString) {
        try {
            FileUtils.deleteDirectory(new File(IOUtils.pathCombine(App.TEMP_FILE_PATH, dateString)));
        } catch (IOException ex) {
            LOGGER.warn("Error while deleting directory: ", ex);
        }
        releaseTemporaryDir(dateString);
    }

    public static void releaseTemporaryDir(String dateString) {
        synchronized (activeDirectories) {
            activeDirectories.remove(dateString);
        }
    }

}
