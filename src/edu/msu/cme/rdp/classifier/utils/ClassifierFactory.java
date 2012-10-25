/*
 * ClassifierFactory.java
 *
 * Copyright 2006 Michigan State University Board of Trustees
 * 
 * Created on November 6, 2003, 10:56 AM
 */
package edu.msu.cme.rdp.classifier.utils;

import edu.msu.cme.rdp.classifier.Classifier;
import edu.msu.cme.rdp.classifier.HierarchyTree;
import edu.msu.cme.rdp.classifier.TrainingDataException;
import edu.msu.cme.rdp.classifier.TrainingInfo;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;

/**
 * A factory to create a classifier with the training information defined by the property file.
 * @author  wangqion
 */
public class ClassifierFactory {

    public static final String RRNA_16S_GENE = "16srrna";
    public static final String FUNGALLSU_GENE = "fungallsu";
    private TrainingInfo trainingInfo;
    private static Properties urlProperties;
    private static String classifierVersion;
    /**The default data directory, relative to the classpath. */
    private static final String dataDir = "/data/classifier/";
    /**The default data property file name, inside the default data directory */
    private static final String defaultDataProp = "rRNAClassifier.properties";
    private static String parentPath;
    private static String dataProp = dataDir + RRNA_16S_GENE + "/" + defaultDataProp;
    private static HashMap<String, ClassifierFactory> classifierFactoryMap = new HashMap<String, ClassifierFactory>(); // key=genename
    private static boolean relativePath = true;

    /** Creates a new instance of ClassifierFactory. */
    private ClassifierFactory(String gene) throws IOException, TrainingDataException {

        if (urlProperties == null) {
            InputStream inStream = null;

            if (relativePath) {
                String gene_dataProp = dataDir + gene + "/" + defaultDataProp;
                URL aurl = this.getClass().getResource(gene_dataProp);
                inStream = this.getClass().getResourceAsStream(gene_dataProp);
                parentPath = new File(aurl.getFile()).getParent();

            } else {
                File aFile = new File(dataProp);
                String absolutePath = aFile.getAbsolutePath();
                parentPath = absolutePath.substring(0, absolutePath.lastIndexOf(File.separatorChar));
                inStream = new FileInputStream(aFile);
            }
            urlProperties = new Properties();
            urlProperties.load(inStream);
            inStream.close();
        }

    }

    /**
     * Resets the data property file to the default data property file.
     */
    public static void resetDefaultDataProp() {
        dataProp = defaultDataProp;
        urlProperties = null;
        classifierFactoryMap = new HashMap<String, ClassifierFactory>();
        relativePath = true;
    }

    /** Sets the property file which contains the mapping of the training files.
     * The actually training data files should be in the same directory as this property file.
     * To override the default property location, this method must be called before 
     * the first ClassifierFactory.getFactory() call. 
     */
    public static void setDataProp(String properties, boolean relative) {
        dataProp = properties.trim();
        urlProperties = null;
        classifierFactoryMap = new HashMap<String, ClassifierFactory>();
        relativePath = relative;
    }

    /**
     *  XXXX this needs to be removed once we modified myRDP to take a gene for classifier
     * @return
     * @throws IOException
     * @throws TrainingDataException
     */
    public synchronized static ClassifierFactory getFactory() throws IOException, TrainingDataException {
        return getFactory(RRNA_16S_GENE);
    }

    /** Returns a factory with the training information.
     * This method initialize all the training information.
     * Note: the ClassifierFactory.setDataProp() static method must be called before  
     * this method if default property file will not be used.
     */
    public synchronized static ClassifierFactory getFactory(String gene) throws IOException, TrainingDataException {
        if (!classifierFactoryMap.containsKey(gene)) {

            if (relativePath) {
                ClassifierFactory factory = new ClassifierFactory(gene);
                factory.trainingInfo = new TrainingInfo();

                InputStreamReader in = new InputStreamReader(ClassifierFactory.class.getResourceAsStream(dataDir + gene + "/" + convert("probabilityList")));
                factory.trainingInfo.createGenusWordProbList(in);

                // the tree information has to be read after at least one of the other
                //three files because we need to set the version information.	           
                in = new InputStreamReader(ClassifierFactory.class.getResourceAsStream(dataDir + gene + "/" + convert("bergeyTree")));
                factory.trainingInfo.createTree(in);

                in = new InputStreamReader(ClassifierFactory.class.getResourceAsStream(dataDir + gene + "/" + convert("probabilityIndex")));
                factory.trainingInfo.createProbIndexArr(in);

                in = new InputStreamReader(ClassifierFactory.class.getResourceAsStream(dataDir + gene + "/" + convert("wordPrior")));
                factory.trainingInfo.createLogWordPriorArr(in);
                factory.classifierVersion = convert("classifierVersion");

                classifierFactoryMap.put(gene, factory);
            } else {
                getNonDefaultFactory(gene);
            }
        }
        return classifierFactoryMap.get(gene);
    }

    /**
     * Returns a factory with the training information for the non-default training data files.
     */
    private static ClassifierFactory getNonDefaultFactory(String gene) throws IOException, TrainingDataException {
        if (classifierFactoryMap.get(gene) == null) {
            ClassifierFactory factory = new ClassifierFactory(gene);
            factory.trainingInfo = new TrainingInfo();
            String filename = parentPath + File.separatorChar + convert("probabilityList");

            FileReader in = new FileReader(filename);
            factory.trainingInfo.createGenusWordProbList(in);
            // the tree information has to be read after at least one of the other
            //three files because we need to set the version information.
            filename = parentPath + File.separatorChar + convert("bergeyTree");
            in = new FileReader(filename);
            factory.trainingInfo.createTree(in);

            filename = parentPath + File.separatorChar + convert("probabilityIndex");
            in = new FileReader(filename);
            factory.trainingInfo.createProbIndexArr(in);

            filename = parentPath + File.separatorChar + convert("wordPrior");
            in = new FileReader(filename);
            factory.trainingInfo.createLogWordPriorArr(in);
            factory.classifierVersion = convert("classifierVersion");

            classifierFactoryMap.put(gene, factory);
        }
        return classifierFactoryMap.get(gene);
    }

    /** Retrieves appropriate value from the property file.
     */
    private static String convert(String key) throws IOException {
        String filename = urlProperties.getProperty(key);
        if (filename == null) {
            throw new IOException("Returns 'null' while retrieving "
                    + key + " from the Properties, Please check your key'.");
        }
        return filename;
    }

    /** Creates a new classifier.
     */
    public Classifier createClassifier() {
        return trainingInfo.createClassifier();
    }

    /** Returns the version of the taxonomical hierarchy.
     */
    public String getHierarchyVersion() {
        return trainingInfo.getHierarchyVersion();
    }

    /** Returns the info of the taxonomy hierarchy from of the training file.
     */
    public HierarchyVersion getHierarchyTrainsetNo() {
        return trainingInfo.getHierarchyInfo();
    }

    /** Returns the version of the classifier.
     */
    public String getClassifierVersion() {
        return classifierVersion;
    }

    public HierarchyTree getRoot() {
        return trainingInfo.getRootTree();
    }
}
