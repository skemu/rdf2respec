package com.skemu.rdf.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorLogger;

public class RdfIoUtil {

    private static final ValueFactory VF = SimpleValueFactory.getInstance();

    public static Model read(File file, RDFFormat format) {
        try (InputStream input = new FileInputStream(file)) {
            return read(input, format);
        } catch (IOException e) {
            throw new RuntimeException(String.format("failed to parse file [%s] as [%s]", file, format), e);
        }
    }

    public static Model read(InputStream inputStream, RDFFormat format) {
        try (InputStream is = inputStream) {
            ParserConfig settings = new ParserConfig();
            settings.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
            return Rio.parse(is, "http://none.com/", format, settings, VF, new ParseErrorLogger());
        } catch (IOException e) {
            throw new RuntimeException(String.format("failed to parse input stream [%s] as [%s]", inputStream, format), e);
        }
    }
}
