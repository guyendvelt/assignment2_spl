package spl.lae;
import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String[] invalidTests= new String[]{
            "test_invalid_dimension_add.json",
            "test_invalid_dimension_multiply.json",
            "test_invalid_empty_array.json",
        };
        // TODO: main
        LinearAlgebraEngine LAE=new LinearAlgebraEngine(10);
        InputParser inputParser=new InputParser();
       
        try{
            ComputationNode root=inputParser.parse(invalidTests[5]);
            LAE.run(root);
            OutputWriter.write(root.getMatrix(), "My_out.json");
        }catch (Exception e) {
            OutputWriter.write(e.getMessage(), "My_out.json");
        }finally{
            try {
                LAE.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
       

    }
}