package il.ac.bgu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.opendevl.JFlat;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by borisk on 12/5/2018.
 */

public class YmlToCsvConverter {

    private static final Logger log;

    static {
        Configurator.initialize(null, "conf/log4j.properties");
        log = LoggerFactory.getLogger(YmlToCsvConverter.class);

    }


    public static void main(String[] args) throws IOException {

        for (File ymlFile : FileUtils.listFiles(new File("stats/"), new String[]{"yml"}, false)) {
            log.info("Start converting {} to csv", ymlFile.getName());

            String jsonString = new ObjectMapper().writeValueAsString(
                    new ObjectMapper(new YAMLFactory()).readValue(ymlFile, Object.class));

            new JFlat(jsonString).json2Sheet().write2csv(new File(ymlFile.getParent(),
                    ymlFile.getName().replace("yml", "csv")).getCanonicalPath());

        }
    }

}
