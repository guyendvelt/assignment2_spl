package spl.lae;

import java.io.IOException;
import java.text.ParseException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        // TODO: main
        LinearAlgebraEngine lae = new LinearAlgebraEngine(10);
        InputParser inputParser = new InputParser();
        try {
            ComputationNode root = inputParser.parse("example.json");
            ComputationNode res = lae.run(root);
            OutputWriter.write(res.getMatrix(), "My_out.json");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            OutputWriter.write("Error: " + e.getMessage(), "My_out.json");
        } finally {
            System.out.println(lae.getWorkerReport());
        }

    }
}