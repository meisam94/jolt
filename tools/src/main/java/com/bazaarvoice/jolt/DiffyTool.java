package com.bazaarvoice.jolt;

import com.fasterxml.jackson.core.JsonParseException;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

public class DiffyTool {

    public static void main (String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser( "diffy" )
                .description( "Jolt CLI Diffy Tool" )
                .defaultHelp( true );

        parser.addArgument( "filePath1" ).help( "File path to first JSON document to be fed to Diffy" )
                .type( Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead() );
        parser.addArgument( "filePath2" ).help( "File path to second JSON document to be fed to Diffy" )
                .type( Arguments.fileType().verifyExists().verifyIsFile().verifyCanRead() );

        parser.addArgument( "-s" ).help( "help for suppress output" )
                .action( Arguments.storeTrue() );
        parser.addArgument( "-a" ).help( "help for array order oblivious" )
                .action( Arguments.storeTrue() );

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        boolean suppressOutput = ns.getBoolean( "s" );

        Diffy diffy;
        if ( ns.getBoolean( "a" ) ) {   // caller wants to disregard array order when diffing
            diffy = new ArrayOrderObliviousDiffy();
        } else {
            diffy = new Diffy();
        }

        Map<String, Object> objectMap1 = createObjectMap( ( File ) ns.get( "filePath1" ), suppressOutput );
        Map<String, Object> objectMap2 = createObjectMap( ( File ) ns.get( "filePath2" ), suppressOutput );

        Diffy.Result result = diffy.diff( objectMap1, objectMap2 );

        if ( result.isEmpty() ) {
            printOutput( suppressOutput, "Diffy found no differences" );
            System.exit( 0 );
        } else {
            try {
                printOutput( suppressOutput, "A difference was found. Diffy expected this:\n" +
                        JsonUtils.toPrettyJsonString( result.expected ) + "\n" +
                        "Diffy found this:\n" +
                        JsonUtils.toPrettyJsonString( result.actual ) );
            } catch ( IOException e ) {
                printOutput( suppressOutput, "A difference was found, but diffy encountered an error while writing the result." );
            } finally {
                System.exit( 1 );
            }
        }
    }

    private static void printOutput( boolean suppressOutput, String output ) {
        if ( !suppressOutput ) {
            System.out.println( output );
        }
    }

    private static Map<String, Object> createObjectMap( File file, boolean suppressOutput ) {
        Map<String, Object> objectMap = null;
        try {
            FileInputStream inputStream = new FileInputStream( file );
            objectMap = (Map<String, Object>) JsonUtils.jsonToObject( inputStream );
            inputStream.close();
            return objectMap;
        } catch ( IOException e ) {
            if ( e instanceof JsonParseException ) {
                printOutput( suppressOutput, "File " + file.getAbsolutePath() + " did not contain properly formatted JSON." );
            } else {
                printOutput( suppressOutput, "Failed to open file: " + file.getAbsolutePath() );
            }
            System.exit( 1 );
        }
        return objectMap;
    }
}
