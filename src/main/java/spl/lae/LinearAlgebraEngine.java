package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced\
        if (computationRoot == null) {
            throw new IllegalArgumentException("computation root should not be null");
        }
        computationRoot.associativeNesting();
        while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
            ComputationNode resolvableNode = computationRoot.findResolvable();
            if(resolvableNode == null){
                throw new IllegalArgumentException("Tree Structure Error: no resolvable node");
            }
            loadAndCompute(resolvableNode);
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor

        //load operand matrix :
        if(node.getNodeType() == ComputationNodeType.MATRIX){
            throw new IllegalArgumentException("can't load and compute a Matrix");
        }
        if(node.getChildren() == null) {
            throw new IllegalArgumentException("can't compute, node have no children");
        }
        ComputationNode left;
        ComputationNode right;
        ComputationNodeType type = node.getNodeType();
        List<Runnable> tasks;
        if(type == ComputationNodeType.ADD){
            if(node.getChildren().size() < 2){
                throw new IllegalArgumentException("can't add, node have less than 2 children");
            }
            left = node.getChildren().get(0);
            leftMatrix.loadRowMajor(left.getMatrix());
            right = node.getChildren().get(1);
            rightMatrix.loadRowMajor(right.getMatrix());
            tasks = createAddTasks();
        }else if(type == ComputationNodeType.MULTIPLY){
            if(node.getChildren().size() < 2){
                throw new IllegalArgumentException("can't multiply, node have less than 2 children");
            }
            left = node.getChildren().get(0);
            leftMatrix.loadRowMajor(left.getMatrix());
            right = node.getChildren().get(1);
            rightMatrix.loadColumnMajor(right.getMatrix());
            tasks = createMultiplyTasks();
        }else if(type == ComputationNodeType.NEGATE){
            left = node.getChildren().get(0);
            leftMatrix.loadRowMajor(left.getMatrix());
            tasks = createNegateTasks();
        }else{
            //type = transpose :
            left = node.getChildren().get(0);
            leftMatrix.loadRowMajor(left.getMatrix());
            tasks = createTransposeTasks();
        }
        executor.submitAll(tasks);
        node.resolve(leftMatrix.readRowMajor());
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        if (leftMatrix == null || rightMatrix == null || leftMatrix.length() == 0 || rightMatrix.length() == 0) {
            throw new IllegalArgumentException("can't add empty Matrix");
        }
        List<Runnable> tasks = new ArrayList<>();
        if(leftMatrix.length() != rightMatrix.length()
                || leftMatrix.get(0).length() != rightMatrix.get(0).length()){
            throw new IllegalArgumentException("can't add, matrix dimensions mismatch");
        }

//        if (!canAdd()) {
//            throw new IllegalArgumentException("can't add matrix with different dimensions");
//        }
//        if (leftMatrix.getOrientation() != rightMatrix.getOrientation()) {
//            SharedMatrix matrixToChange;
//            if (leftMatrix.getOrientation() == VectorOrientation.COLUMN_MAJOR) {
//                matrixToChange = leftMatrix;
//            } else {
//                matrixToChange = rightMatrix;
//            }
//            double[][] temp = matrixToChange.readRowMajor();
//            matrixToChange.loadRowMajor(temp);
//        }
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).add(rightMatrix.get(row));
            });
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        if (leftMatrix == null || rightMatrix == null || leftMatrix.length() == 0 || rightMatrix.length() == 0) {
            throw new IllegalArgumentException("can't multiply empty Matrix");
        }
        if(leftMatrix.get(0).length() != rightMatrix.get(0).length()){
            throw new IllegalArgumentException("can't multiply, matrices of different dimensions");
        }
//        if(!canMultiply()){
//            throw new ArithmeticException("Can't multiply matrix with different dimensions");
//        }
//        if (leftMatrix.getOrientation() != VectorOrientation.ROW_MAJOR) {
//            double[][] temp = leftMatrix.readRowMajor();
//            leftMatrix.loadRowMajor(temp);
//        }
//        if (rightMatrix.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
//            double[][] temp = rightMatrix.readRowMajor();
//            rightMatrix.loadColumnMajor(temp);
//        }
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).vecMatMul(rightMatrix);
            });
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> tasks = new ArrayList<>();
        if (leftMatrix == null || leftMatrix.length() == 0) {
            throw new IllegalArgumentException("can't negate empty Matrix");
        }
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).negate();
            });
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> tasks = new ArrayList<>();
        if (leftMatrix == null || leftMatrix.length() == 0) {
            throw new IllegalArgumentException("can't transpose empty Matrix");
        }
        for (int i = 0; i < leftMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> {
                leftMatrix.get(row).transpose();
            });
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }

    private boolean canAdd() {
        if (leftMatrix.getOrientation() == rightMatrix.getOrientation()) {
            return leftMatrix.length() == rightMatrix.length()
                    && leftMatrix.get(0).length() == rightMatrix.get(0).length();
        } else {
            return leftMatrix.length() == rightMatrix.get(0).length()
                    && leftMatrix.get(0).length() == rightMatrix.length();
        }
    }

    private boolean canMultiply(){
        VectorOrientation leftOrientation = leftMatrix.getOrientation();
        VectorOrientation rightOrientation = rightMatrix.getOrientation();
        if(leftOrientation == rightOrientation){
            if(leftOrientation == VectorOrientation.ROW_MAJOR){
                return leftMatrix.get(0).length() == rightMatrix.length();
            } else {
                return leftMatrix.length() == rightMatrix.get(0).length();
            }
        } else {
            if(leftOrientation == VectorOrientation.ROW_MAJOR){
                return leftMatrix.get(0).length() == rightMatrix.get(0).length();
            } else {
                return leftMatrix.length() == rightMatrix.length();
            }
        }
    }

    public void shutdown() throws InterruptedException {
        try {
            executor.shutdown();
        }
        catch (InterruptedException e) {
            throw new InterruptedException("Executor shutdown interrupted: " + e.getMessage());
        }

    }




}
