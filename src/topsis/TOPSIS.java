/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package topsis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author andrea
 */
public class TOPSIS {
    
    static File currentDataFile = null;
    static ArrayList<ArrayList<Double>> criteria = null;
    static ArrayList<Double> lp = null;
    static ArrayList<ArrayList<Double>> criteriaVectors = null;
    static ArrayList<ArrayList<ArrayList<Double>>> twobytwoMatrixes = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        int opt = 0;
        
        do {
            System.out.println("---AHP SORT---");
            opt = showMainMenu();
            
            switch (opt){
                case 1: //Solve class problems
                    //TODO
                    File file1 = new File("ej1.txt");
                    File file2 = new File("ej2.txt");

                    if (!file1.exists() || !file2.exists()){
                        System.out.println("No se puede usar esta función porque no se encuentran los archivos originales del problema.");
                        break;
                    } 
                    
                    currentDataFile = file1;
                    System.out.println("Ejercicio B:");
                    
                    fileToMatrix();
                    printData();
                    solveWithCurrentData();
                    
                    checkConsistency();
                    
                    currentDataFile = file2;
                    System.out.println("Ejercicio C:");
                    
                    fileToMatrix();
                    printData();
                    solveWithCurrentData();
                    
                    checkConsistency();
  
                    
                    break;
                case 2: //Load custom file
                    showLoadingFileMenu();
                    break;
                case 3: //Solve using custom file's data
                    if (currentDataFile == null){
                        opt = -1;
                        break;
                    }
                    
                    printData();
                    
                    solveWithCurrentData();
                    
                    checkConsistency();
                
            }
            
        } while (opt != 0);
    }
    
    /**
     * It shows the main menu and gets the user's input.
     * 
     * It also checks if the input is valid. If it isn't, the user is asked again.
     * @return opcion option chosen by the user
     */
    private static int showMainMenu(){
        int option = 0;
        do{
            System.out.println("Elije una de las siguientes opciones:");
            System.out.println("1. Resolver ejercicio de clase (no necesita cargar fichero)");
            System.out.print("2. Cargar fichero de datos");

            if(currentDataFile != null) System.out.println(" (Archivo actual: " + currentDataFile.getName() + ")");
            else System.out.println(" (No hay un archivo cargado)");

            if (currentDataFile != null) System.out.println("3. Resolver usando datos cargados.");
            
            System.out.println("0. Salir.");
            System.out.println();
            Scanner keyboard = new Scanner(System.in);
            System.out.print("¿Cual es tu selección? ");

            try {
                option = Integer.parseInt(keyboard.next());
            } catch(NumberFormatException e) {
                System.out.println("La opción escogida no es un número.");
                option = -1;
            }
            
        }while(option < 0 || option > 3);
        
        return option;
    }
    
    /**
     * It shows the menu for loading a custom file. 
     * 
     * It handles the possible errors which can appear. If a file is stored,
     * it must exist.
     */
    private static void showLoadingFileMenu() throws IOException{
        System.out.println();
        System.out.println("---Cargar fichero---");
        System.out.println("Instrucciones para crear un archivo de valores.");
        System.out.println("En primer lugar, hay que especificar si se va a proporcional los valores reales de desempeño, o el grado de desempeño con el limiting profile (en la escala de Saaty). Si se trata de los valores reales, se escribe en la primera línea un 0. De lo contrario, un 1.");
        System.out.println("-Segunda línea: Número de criterios.");
        System.out.println("-Tercera línea: Número de alternativas.");
        System.out.println("-Cuarta línea: Línea en blanco");
        System.out.println("-Quinta línea: Matriz de criterios, separando los valores con comas e indicando el fin de una línea con un guión. \nEjemplo de matriz identidad 2x2: 1,0-0,1");
        System.out.println("-Sexta línea: Lista de perfiles límite ordenados de la misma forma en la que se presenten los criterios");
        System.out.println("-Sucesivas líneas(sin líneas en blanco): Vectores de desempeño de cada alternativa, reales o respecto al lp");
        System.out.println("-Para ver un ejemplo, consulta el archivo sample_input.txt");
        
        Scanner keyboard = new Scanner(System.in);
        File file = null;
        boolean exit = true;
        do {
            System.out.print("Nombre del archivo (escribe 0 para volver atrás): ");
            file = new File(keyboard.next());
            
            if (!file.getName().equals("0")){
                if (!file.exists()){
                    System.out.println("El archivo no existe.");
                    exit = false;
                } else {
                    currentDataFile = file;
                    fileToMatrix();
                    exit = true;
                }
            } else {
                exit = true;
            }
            
        } while(!exit);

    }
    
    /**
     * It stores all the information in currentDataFile.
     * 
     * The result is a set of matrixes(Criteria) and vectors (lps)
     * 
     */
    private static void fileToMatrix(){
        if (!currentDataFile.exists()){
            System.out.println("El archivo cargado ya no existe. Por favor, cárgalo de nuevo. \n");
            return;
        }
        
        try{
            
            FileReader fr = new FileReader(currentDataFile);
            BufferedReader br = new BufferedReader(fr);
            
            int type = Integer.parseInt(br.readLine());
            int numCriteria = Integer.parseInt(br.readLine());
            int numAlternatives = Integer.parseInt(br.readLine());
            
            br.readLine(); //Skip empty line
            
            criteria = stringToMatrix(br.readLine()); //Store criteria matrix
            
            ArrayList<Double> lpTemp = new ArrayList<>();
            
            String[] lps = br.readLine().split(",");
            for (int i = 0; i < lps.length; i++) lpTemp.add(Double.parseDouble(lps[i]));
            
            lp = lpTemp; //Store limiting profiles
            
            criteriaVectors = new ArrayList<>();
            for (int i = 0; i < numCriteria; i++){
               
                lps = br.readLine().split(",");
                
                if (type == 0) toSaatyScale(lps,i);
                criteriaVectors.add(new ArrayList<>());
                for (int j = 0; j < lps.length; j++) criteriaVectors.get(i).add(Double.parseDouble(lps[j]));
                
                
                if (i == numCriteria && criteriaVectors.get(0).size() != i+1){ //Check if file was wrongly written
                    throw new Exception();
                }
            }

        } catch (Exception e){
            System.out.println("Ha habido un problema con el archivo.");
            currentDataFile = null;
            criteria = null;
            criteriaVectors = null;
            lp = null;
        } 
    }  
    
    /**
     * It turns an string into a matrix following certain rules.
     * 
     * A given string will contain values separated by ','. Each row will
     * be divided by a '-'
     * @param s string to turn into a matrix
     * @return Matrix
     */
    private static ArrayList<ArrayList<Double>> stringToMatrix(String s){
        String[] rows = s.split("-");
        ArrayList<ArrayList<Double>> m = new ArrayList<>();

        for (int j = 0; j < rows.length; j++){
            m.add(new ArrayList<>());

            String[] values  = rows[j].split(",");

            for (int k = 0; k< values.length; k++){
                m.get(j).add(Double.parseDouble(values[k]));
            }

        }
        
        return m;
    }
    
    /**
     * It shows on the screen the matrix given
     * @param m Matrix to be shown
     */
    private static void printMatrix(ArrayList<ArrayList<Double>> m){
        for (int i = 0;i < m.size(); i++){
            for (int j = 0; j < m.get(i).size(); j++){
                System.out.print(m.get(i).get(j)+"  ");
            }
            
            System.out.println();
        }
        
        System.out.println();
    }
    
    /**
     * It shows on the screen the loaded file's data
     */
    private static void printData(){
        
        if (lp == null || criteria == null || criteriaVectors == null){
            System.out.println("Ha habido un error. Carga el archivo de nuevo.");
            lp = null;
            criteria = null;
            criteriaVectors = null;
            return;
        }
        
        System.out.println("Estos son los datos del archivo "+currentDataFile.getName());
        System.out.println("Matriz de comparación entre criterios:");
        printMatrix(criteria);
        System.out.println("Vector de perfiles límite");
        
        System.out.print("[ ");
        for (int i = 0; i < lp.size(); i++){
            System.out.print(lp.get(i)+"  ");
        }
        System.out.println(" ]");
        System.out.println("Vectores de comparación de cada alternativa según cada criterio respecto al lp: ");
        
        for (int i = 0; i < criteriaVectors.size(); i++){
            System.out.println("Criterio "+(i+1)+": ");
            System.out.print("[ ");
            for (int j = 0; j < criteriaVectors.get(0).size(); j++){
                System.out.print(criteriaVectors.get(i).get(j)+"  ");
            }
            System.out.println(" ]");
        }
        
        System.out.println();
    }
    
    
    /**
     * It returns a Double[][] from ArrayList<ArrayList<Double>>
     * @param matrix 
     */
    private static double[][] toStaticMatrix(ArrayList<ArrayList<Double>> matrix){
        double m[][] = new double[matrix.size()][matrix.get(0).size()];
        
        for (int i = 0; i < matrix.size(); i++){
            for (int j = 0; j < matrix.get(i).size(); j++){
                m[i][j] = matrix.get(i).get(j);
            }
        }
        
        return m;
    }
    
    private static void solveWithCurrentData(){
        int numCriteria = criteriaVectors.size();
        int numAlternatives = criteriaVectors.get(0).size();
        //FIRST we get criteria weights
        ArrayList<Double> criteriaWeights = getEigenVector(criteria);
        
        //SECOND we compute 2x2 matrixes
        computeTwoByTwoMatrixes();
        
        //THIRD aggregate sums
        if (twobytwoMatrixes == null){
            System.out.println("Ha habido un error.");
            criteria = null;
            criteriaVectors = null;
            currentDataFile = null;
            lp = null;
            twobytwoMatrixes = null;
            return;
        }
        
        ArrayList<ArrayList<Double>> tempVectors = new ArrayList<>();
        for (int i = 0; i < twobytwoMatrixes.size(); i++){        
            ArrayList<Double> aux = getEigenVector(twobytwoMatrixes.get(i));
            tempVectors.add(aux);
        }
 
        
        //FOURTH we check if each alternative fits or not the limit
        ArrayList<Integer> pass = new ArrayList<>();
        double sum1,sum2;

        
        for (int i = 0; i < tempVectors.size(); i+=criteriaVectors.size()){
            sum1 = 0;
            sum2 = 0;  
            for (int j = i; j < i+numCriteria; j++){
                sum1 += tempVectors.get(j).get(0)*criteriaWeights.get(j%criteriaWeights.size());
                sum2 += tempVectors.get(j).get(1)*criteriaWeights.get(j%criteriaWeights.size());
            }
            
            if (sum1 >= sum2) pass.add(i/numCriteria + 1);
        }
       
        System.out.print("Alternativas que pasan: \n[ ");
        for (int i = 0; i < pass.size(); i++){
            System.out.print(pass.get(i) + "  ");
        }
        
        System.out.println(" ]");
        
    }
    
    /**
     * It computes the two by two matrixes needed
     */
    private static void computeTwoByTwoMatrixes(){
        twobytwoMatrixes = new ArrayList<>();
        
        for (int i = 0; i < criteriaVectors.get(0).size(); i++){ //Each alternative
            for (int j = 0; j < criteriaVectors.size(); j++){ //Each criterion
                ArrayList<ArrayList<Double>> aux= new ArrayList<ArrayList<Double>>();
                
                aux.add(new ArrayList<>());
                aux.add(new ArrayList<>());
                
                aux.get(0).add(1.0);
                aux.get(0).add(criteriaVectors.get(j).get(i));
                aux.get(1).add(1/criteriaVectors.get(j).get(i));
                aux.get(1).add(1.0);
                
                twobytwoMatrixes.add(aux);
            }
        }
    }
    
    /**
     * Given a matrix, it returns its eigenvector
     * @param m matrix
     * @return eigenvector
     */
    private static ArrayList<Double> getEigenVector(ArrayList<ArrayList<Double>> m){
        double[][] values = toStaticMatrix(m);
        
        RealMatrix matrix=MatrixUtils.createRealMatrix(values);
        EigenDecomposition descomposition=new EigenDecomposition(matrix);
        
        double[] eigenValues=descomposition.getRealEigenvalues();
        int max = 0;
        
        for (int i = 1; i < eigenValues.length; i++){
            if (eigenValues[i] > eigenValues[max]) max = i;
        }
        
        RealVector eigenVector=descomposition.getEigenvector(max);
        
        
        //Normalize
        double sum = 0;
        for (int i = 0; i < eigenVector.getDimension(); i++){
            sum += eigenVector.getEntry(i);
        }
        
        ArrayList<Double> vector = new ArrayList<>();
        
        for (int i = 0; i < eigenVector.getDimension(); i++){
            vector.add(eigenVector.getEntry(i)/sum);
        }
        
        return vector;
    }
    
    /**
     * Given an array of string lps, it turns it into Saaty scale.
     * @param lps 
     */
    private static void toSaatyScale(String[] lps, int criterion){
        double aux,limit;
        
        for (int i = 0; i < lps.length; i++){
            aux = Double.parseDouble(lps[i]);
            limit = lp.get(criterion);
            
            if (aux > limit){
                if (aux < limit+5) aux = 3;
                else if (aux < limit+10) aux = 5;
                else if (aux < limit+15) aux = 7;
                else aux = 9;
            } else if (aux < limit){
                if (aux > limit-5) aux = 1.0/3.0;
                else if (aux > limit-10) aux = 1.0/5.0;
                else if (aux > limit-15) aux = 1.0/7.0;
                else aux = 1.0/9.0;
            } else aux = 1;
            
            lps[i] = String.valueOf(aux);
        }      
        
    }
    
    /**
     * If a criteria matrix is loaded, it is shown on the screen its consistency.
     */
    private static void checkConsistency(){
        RealMatrix matrix=MatrixUtils.createRealMatrix(toStaticMatrix(criteria));
        EigenDecomposition descomposition=new EigenDecomposition(matrix);
        
        double[] eigenValues=descomposition.getRealEigenvalues();
        int max = 0;
        double maxEV = eigenValues[0];
        
        for (int i = 1; i < eigenValues.length; i++){
            if (eigenValues[i] > eigenValues[max]){
                max = i;
                maxEV = eigenValues[max];
            }
        }
        
        Double ci = (maxEV-eigenValues.length) / (eigenValues.length-1);
        
        //Random indexes (starting from n = 2)
        double[] ri = new double[9];
        ri[0] = 0.58; //Made up by me
        ri[1] = 0.58;
        ri[2] = 1.9;
        ri[3] = 1.12;
        ri[4] = 1.24;
        ri[5] = 1.32;
        ri[6] = 1.41;
        ri[7] = 1.44;
        ri[8] = 1.49;
        //Return consistency ratio
        
        Double cr = ci/ri[criteria.size()-2];
        
        System.out.println("Consistencia de los datos: ");
        System.out.println("Ratio de consistencia: "+cr);
        if (cr > 0.1) System.out.println("Los datos no son consistentes, por lo que los resultados no son fiables.");
        else System.out.println("Los datos son consistentes, por lo que se puede confiar en la clasificación obtenida.");
        System.out.println();
    }
    

}
