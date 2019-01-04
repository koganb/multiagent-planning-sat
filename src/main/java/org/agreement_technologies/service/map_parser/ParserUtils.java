package org.agreement_technologies.service.map_parser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.stream.Collectors;

public class ParserUtils {

    public static String readToString(String fileName) throws IOException {
        String classpathFilename = fileName.trim().startsWith("/") ? fileName : "/" + fileName;

        if (MAPDDLParserImp.class.getResource(classpathFilename) != null) {
            return IOUtils.readLines(MAPDDLParserImp.class.getResourceAsStream(classpathFilename), Charset.defaultCharset())
                    .stream().collect(Collectors.joining("\n"));
        } else if (new File(fileName).exists()) {
            return FileUtils.readFileToString(new File(fileName), Charset.defaultCharset());
        }
        throw new RuntimeException("Path: " + fileName + " was not found neither in classpath nor in filesystem");
    }

}
