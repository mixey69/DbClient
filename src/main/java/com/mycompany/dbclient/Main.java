/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.dbclient;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

/**
 *
 * @author mac
 */
public class Main {

    public static Connection connection = null;
    public static final String SELECT_STATEMENT = "SELECT text FROM pics"; // WHERE ID<=10";
    public static final String DATE_PATTERN = "date=((?:.|\\n)*?),";
    public static final String DESC_PATTERN = "text='((?:.|\\n)*?)',";
    public static final String PIC_ID_PATTERN = "\\{id=((?:.|\\n)*?),";
    public static final String ALBUM_ID_PATTERN = "albumId=((?:.|\\n)*?),";
    public static final String URL_PATTERN = "(https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))";
    static Pattern descPattern = Pattern.compile(DESC_PATTERN);
    static Pattern datePattern = Pattern.compile(DATE_PATTERN);
    static Pattern picIdPattern = Pattern.compile(PIC_ID_PATTERN);
    static Pattern albumIdPattern = Pattern.compile(ALBUM_ID_PATTERN);
    static Pattern urlPattern = Pattern.compile(URL_PATTERN);
    static Statement askingStatement;
    static ResultSet resultSet;
    static Integer hover = 32423;
    static Feature[] test;
    public static final Main m = new Main();

    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:/Applications/deadPeople.db");
            askingStatement = connection.createStatement();
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        //testing first example
        int lengthOfFeatureListList = 0;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT ID FROM DESCRIPTIONS WHERE CAUSE_OF_DEATH > ?");
            statement.setInt(1, -1);
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                lengthOfFeatureListList++;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        double[] targetValues = new double[lengthOfFeatureListList];
        Feature[][] data = prepareData();
        //Feature[][] testData = prepareTestData(1);
        
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT CAUSE_OF_DEATH FROM DESCRIPTIONS WHERE CAUSE_OF_DEATH > ?");
            statement.setInt(1, -1);
            ResultSet resSet = statement.executeQuery();
            int index = 0;
            while (resSet.next()) {
                targetValues[index] = resSet.getInt("CAUSE_OF_DEATH");
                index++;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (Feature[] dataSet : prepareTestData(1)){
            train(targetValues, data, dataSet);
        }

        
        //mapFeatures();
        //createFeatureTable();
        //makeAMap2();
        //makeAMap();
        //cleanDesc();
        //askAndIterate();
    }
    
    public static int getWeight(){
        int i = 76;
        return i;
    }

    public static void train(double[] targetValues, Feature[][] features, Feature[] instance) {
        Problem problem = new Problem();
        problem.l = features.length; // number of training examples
        problem.n = 3304; // number of features
        problem.x = features; // feature nodes
        problem.y = targetValues;//{0, 1, 2, 3, 4, 5, 6, 7, 8}; // target values

        Model model = new Model();

        SolverType solver = SolverType.L2R_L1LOSS_SVC_DUAL; // -s 0
        double C = 1.0;    // cost of constraints violation
        double eps = 0.01; // stopping criteria

        Parameter parameter = new Parameter(solver, C, eps);
        model = Linear.train(problem, parameter);
        File modelFile = new File("model");
         try {
            model.save(modelFile);
//   load model or use it directly
           // model = Model.load(modelFile);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println(instance);
//        Feature[] instance = {new FeatureNode(1, 4), new FeatureNode(2, 2)};
        double prediction = Linear.predict(model, instance);
        System.out.println("prediction is: " + prediction);
    }

    public static Feature[][] prepareTestData(int quantity) {
        Feature[] resultForOne = new Feature[3304];
        ArrayList ids = getFeatureArray();
        ArrayList<Integer> halfBooleanArray = new ArrayList(3304);
        for (int ind = 0; ind < 3304; ind++) {
            halfBooleanArray.add(0);
        }
        int iterator = 0;
        int IndexOfFeatureList = 0;
        Feature[][] result = new Feature[quantity][];
        System.out.println(result.length + " - length of test array");
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT FEATURES, ID FROM DESCRIPTIONS WHERE CAUSE_OF_DEATH IS NULL AND ID > ?");
            statement.setInt(1, 3289);
            resultSet = statement.executeQuery();

            while (resultSet.next() && iterator < quantity) {
                iterator++;
                String featureIDs = resultSet.getString("FEATURES");
                System.out.println(featureIDs);
                String[] features = featureIDs.split(" ");
                for (String feature : features) {
                    //Filling array with int markers for IDs
                    int ID = Integer.parseInt(feature);
                    int index = ids.indexOf(ID);
                    halfBooleanArray.set(index, halfBooleanArray.get(index) + 1);
                }

                for (int i = 1; i < 3305; i++) {
                    resultForOne[i - 1] = new FeatureNode(i, halfBooleanArray.get(i - 1));
                }
                result[IndexOfFeatureList] = resultForOne;
                IndexOfFeatureList++;

            }

        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;

    }

    /*
    prepareData() returns all feature lists for which there are causes of death
     */
    public static Feature[][] prepareData() {
        Feature[] resultForOne = new Feature[3304];
        ArrayList ids = getFeatureArray();
        ArrayList<Integer> halfBooleanArray = new ArrayList(3304);
        for (int ind = 0; ind < 3304; ind++) {
            halfBooleanArray.add(0);
        }
        int lengthOfFeatureList = 0;
        int IndexOfFeatureList = 0;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT ID FROM DESCRIPTIONS WHERE CAUSE_OF_DEATH > ?");
            statement.setInt(1, -1);
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                lengthOfFeatureList++;
            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        Feature[][] result = new Feature[lengthOfFeatureList][];
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT FEATURES FROM DESCRIPTIONS WHERE CAUSE_OF_DEATH > ?");
            statement.setInt(1, -1);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String featureIDs = resultSet.getString("FEATURES");
                String[] features = featureIDs.split(" ");
                for (String feature : features) {
                    //Filling array with int markers for IDs
                    int ID = Integer.parseInt(feature);
                    int index = ids.indexOf(ID);
                    halfBooleanArray.set(index, halfBooleanArray.get(index) + 1);
                }

                for (int i = 0; i < 3304; i++) {
                    resultForOne[i] = new FeatureNode(i + 1, halfBooleanArray.get(i));
                }
                result[IndexOfFeatureList] = resultForOne;
                IndexOfFeatureList++;

            }

        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;

    }

    public static ArrayList getFeatureArray() {
        ArrayList<Integer> result = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT MAP_FEATURE_ID FROM FEATURES");
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                result.add(resultSet.getInt("MAP_FEATURE_ID"));
            }

        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }

    public static void mapFeatures() {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT FEATURE_ID FROM MAP_2 WHERE COUNT>?");
            statement.setInt(1, 2);

            resultSet = statement.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            while (resultSet.next()) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO FEATURES(MAP_FEATURE_ID) VALUES(?)");
                preparedStatement.setInt(1, resultSet.getInt("FEATURE_ID"));
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void createFeatureTable() {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT VALUE, FEATURE_ID, COUNT FROM MAP_2 WHERE COUNT>?");
            statement.setInt(1, 2);

            resultSet = statement.executeQuery();
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            int i = 0;
            int count = 0;
            int left = 89547;
            while (resultSet.next()) {
                i++;
                int countI = resultSet.getInt("COUNT");
                count = count + countI;
                left = left - countI;
                System.out.println("feature #" + i + " count = " + countI + " already done: " + count + " left: " + left);
                Integer featureID = resultSet.getInt("FEATURE_ID");
                // System.out.println(featureID);
                String stringIDs = resultSet.getString("VALUE");
                for (String id : stringIDs.split(" ")) {
                    PreparedStatement idStatement = connection.prepareStatement("SELECT FEATURES FROM DESCRIPTIONS WHERE ID = ?");
                    idStatement.setString(1, id);
                    ResultSet featuresByID = idStatement.executeQuery();
                    String features = featuresByID.getString("FEATURES");
                    // System.out.println(features + " features got from person #"+id);
                    if (features == null || features.equals("")) {
                        features = "";
                    }
                    features = features + featureID.toString() + " ";
                    // System.out.println(features + " features that get set");
                    PreparedStatement preparedStatement = connection.prepareStatement("UPDATE DESCRIPTIONS SET FEATURES = ? WHERE ID = ?");
                    preparedStatement.setString(1, features);
                    preparedStatement.setInt(2, Integer.parseInt(id));
                    preparedStatement.executeUpdate();
                }

            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void makeAMap2() {
        try {
            resultSet = askingStatement.executeQuery("SELECT DESC, ID FROM DESCRIPTIONS");
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        HashMap<String, ArrayList<String>> map = new HashMap();
        try {
            while (resultSet.next()) {

                String desc = resultSet.getString("DESC");
                Integer intID = resultSet.getInt("ID");
                String id = intID.toString();
                desc = desc.replace("\\s+", " ");
                String[] wordList = desc.split(" ");
                for (String word : wordList) {
                    word = Porter.stem(word);
                    ArrayList<String> IDs = map.get(word);
                    if (IDs == null) {
                        IDs = new ArrayList<>();
                    }
                    IDs.add(id);
                    map.put(word, IDs);
                }
            }
            for (Map.Entry<String, ArrayList<String>> entry : map.entrySet()) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO MAP_2(KEY, VALUE, COUNT) VALUES(?,?,?)");
                preparedStatement.setString(1, entry.getKey());
                String IDs = "";
                for (String ID : entry.getValue()) {
                    IDs = IDs + ID + " ";
                }
                preparedStatement.setString(2, IDs);
                preparedStatement.setInt(3, entry.getValue().size());
                preparedStatement.executeUpdate();
            }

        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void makeAMap() {
        try {
            resultSet = askingStatement.executeQuery("SELECT DESC FROM DESCRIPTIONS");
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        HashMap<String, Integer> map = new HashMap();
        try {
            while (resultSet.next()) {
                String desc = resultSet.getString("DESC");
                desc = desc.replace("  ", " ");
                String[] wordList = desc.split(" ");
                for (String word : wordList) {
                    Integer frequency = map.get(word);
                    map.put(word, frequency == null ? 1 : frequency + 1);
                }
            }
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO MAP(KEY, VALUE) VALUES(?,?)");
                preparedStatement.setString(1, entry.getKey());
                preparedStatement.setInt(2, entry.getValue());
                preparedStatement.executeUpdate();
            }

        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void cleanDesc() {
        try {
            resultSet = askingStatement.executeQuery("SELECT DESC,ID FROM DEAD_PEOPLE WHERE DESCRIBED>0");
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            while (resultSet.next()) {
                String desc = resultSet.getString("DESC");
                String id = resultSet.getString("ID");
                String urls = " ";
                String insertStatement = "INSERT INTO DESCRIPTIONS(DESC, LINKS, HAS_LINKS, ID) VALUES(?,?,?,?)";
                PreparedStatement preparedStatement = connection.prepareStatement(insertStatement);
                Matcher matcher = urlPattern.matcher(desc);
                Matcher stupidMatcher = Pattern.compile("([-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*))").matcher(desc);
                if (matcher.find()) {
                    matcher = matcher.reset();
                    while (matcher.find()) {
                        desc = desc.replace(matcher.group(1), "");
                        urls = urls + matcher.group(1) + " ";
                    }
                    preparedStatement.setInt(3, 1);
                    matcher = matcher.reset();
                } else if (stupidMatcher.find() && !matcher.find()) {
                    stupidMatcher = stupidMatcher.reset();
                    while (stupidMatcher.find()) {
                        desc = desc.replace(stupidMatcher.group(), "");
                        urls = urls + "https://" + stupidMatcher.group() + " ";
                    }
                    preparedStatement.setInt(3, 2);
                } else {
                    preparedStatement.setInt(3, 0);
                }
                desc = desc.toLowerCase();
                Pattern pattern = Pattern.compile("([^а-ё ]+)");
                Matcher wordMatcher = pattern.matcher(desc);
                //String newDesc = "";
                while (wordMatcher.find()) {
                    desc = desc.replace(wordMatcher.group(), " ");
                    wordMatcher = pattern.matcher(desc);
                }
                desc = desc.replace("  ", " ");
                preparedStatement.setString(1, desc);
                preparedStatement.setString(2, urls);
                preparedStatement.setString(4, id);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void askAndIterate() {
        try {
            resultSet = askingStatement.executeQuery(SELECT_STATEMENT);
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            while (resultSet.next()) {
                insert(resultSet.getString("text"));

            }
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void insert(String picInfo) {
        Matcher descMatcher = descPattern.matcher(picInfo);
        Matcher dateMatcher = datePattern.matcher(picInfo);
        Matcher picIdMatcher = picIdPattern.matcher(picInfo);
        Matcher albumIdMatcher = albumIdPattern.matcher(picInfo);

        descMatcher.find();
        dateMatcher.find();
        picIdMatcher.find();
        albumIdMatcher.find();

        int described = 1;
        if (descMatcher.group(1).equals("") || descMatcher.group(1).equals(" ")) {
            described = 0;
        }

        try {
            String sql = "INSERT INTO DEAD_PEOPLE(DESC, DESCRIBED, DATE, PHOTO_ID, ALBUM_ID) VALUES(?,?,?,?,?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, descMatcher.group(1));
            pstmt.setInt(2, described);
            pstmt.setInt(3, Integer.parseInt(dateMatcher.group(1)));
            pstmt.setInt(4, Integer.parseInt(picIdMatcher.group(1)));
            pstmt.setInt(5, Integer.parseInt(albumIdMatcher.group(1)));
            pstmt.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
