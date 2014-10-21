/*
 * Copyright (C) 2014 wangqion
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package edu.msu.cme.rdp.multicompare;

import edu.msu.cme.rdp.multicompare.taxon.MCTaxon;
import edu.msu.cme.rdp.multicompare.visitors.DefaultPrintVisitor;
import edu.msu.cme.rdp.taxatree.ConcretRoot;
import edu.msu.cme.rdp.taxatree.TaxonHolder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author wangqion
 */
public class MergeTaxonCount {
    
    private MultiClassifierResult result = null;
    public MultiClassifierResult getResult(){
        return result;
    }
    
    public void processOneHierFile(String file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null; 
        ArrayList<String[]> oneHierBlock = new ArrayList<String[]> () ;
        while ( (line= reader.readLine())!= null){
            if ( line.trim().equals("")) continue;
            if ( line.startsWith("taxid")){
                if ( oneHierBlock.size() > 0){
                    processOneHierBlock(oneHierBlock);
                }
                oneHierBlock = new ArrayList<String[]> ();
            }
            oneHierBlock.add(line.split("\\t"));
        }
        if ( oneHierBlock.size() > 0){
            processOneHierBlock(oneHierBlock);
        }
        reader.close();
    }
    
    public void processOneHierBlock( ArrayList<String[]> oneHierBlock) throws IOException{
        
        // get the samples names
        int offset = 4;
        String[] values = oneHierBlock.get(0);
        List<MCSample> curSampleList = new ArrayList<MCSample>();
        for ( int i = offset; i< values.length; i++){
            if ( result != null){
                MCSample sample = result.getSample(values[i]);
                if( sample == null){
                    sample = new MCSample(values[i]);
                    result.addSample(sample);
                }
                curSampleList.add(sample);
            }else {
                MCSample sample = new MCSample(values[i]);
                curSampleList.add(sample);
            }
        }
                
        //taxid	lineage	name	rank sample sample .....
        // the first line should be the root, we need to handle this specially because a previous bug printed out the root taxon differenly
        values = oneHierBlock.get(1);
        if( result == null){
            // the first line should be the root taxon
             ConcretRoot root = new ConcretRoot<MCTaxon>(new MCTaxon(Integer.parseInt(values[0]), values[2], values[3]) );
             result = new MultiClassifierResult(root);
             result.addSampleList(curSampleList);
        }
        MCTaxon curTaxon = (MCTaxon)(result.getRoot().getRootTaxonHodler().getTaxon());
        // add counts
        for ( int i = offset; i< values.length; i++){
            curTaxon.incCount(curSampleList.get(i-offset), Double.parseDouble(values[i]));
        }
        
        for ( int ln = 2; ln < oneHierBlock.size(); ln++){
            values = oneHierBlock.get(ln);
            int taxid = Integer.parseInt(values[0]);
            
            // find the parent taxon
            String[] lineage = values[1].split(";");
            TaxonHolder parentTaxon = result.getRoot().getRootTaxonHodler();
            for ( int i= 2; i< lineage.length-2; i+=2){ // the first taxon should be the root
                TaxonHolder temp = parentTaxon.getImediateChildTaxon(lineage[i]);
                if ( temp == null){
                    throw new IOException("Error: Something is wrong with input file, can not find parent node " + lineage[i] + " in line: "+ values[1] );
                }
                parentTaxon = temp;
            }

            //check if the name and rank match the existing one, in case the result from different version
            TaxonHolder tempChild = parentTaxon.getImediateChildTaxon(values[2]);
            if ( tempChild != null){
                curTaxon = (MCTaxon) tempChild.getTaxon();

                if ( curTaxon.getTaxid() != taxid ){
                    throw new IOException("Error: Something is wrong with input file: taxon name " + values[2] + " with taxid " + taxid
                    + " does not match previous processed taxon " + curTaxon.getName() + " with taxid " + curTaxon.getTaxid()
                    + ". Possibly from different training sets ??");
                }
            }else {
                curTaxon = new MCTaxon(taxid, values[2], values[3], false);
                result.getRoot().addChild(curTaxon, parentTaxon.getTaxon().getTaxid());
                curTaxon.setLineage(values[1]);
            }
            // add counts
            for ( int i = offset; i< values.length; i++){
                curTaxon.incCount(curSampleList.get(i-offset), Double.parseDouble(values[i]));
            }
        }

    }
    
    
    public static void main(String[] args) throws Exception{
        String usage = "Usage: out_merged_count.txt in_taxoncount.txt n_taxoncount.txt ...\n" + 
                "  This program merges multiple taxon count files to into one count file, keeping one column for each unique sample.\n" +
                "  If same sample occurred more than once, the taxon count for this sample will be combined.";
        if (args.length < 2){
            System.err.println(usage);
            System.exit(1);
        }
        File outFile = new File(args[0]);
        if (  outFile.exists()){
            System.err.println("Error: output file " + args[0] + " already exists!");
            System.exit(1);
        }
        
        try {
            MergeTaxonCount theObj = new MergeTaxonCount();
            for ( int i = 1; i < args.length; i++){
                theObj.processOneHierFile(args[i]);
            }

            PrintStream hier_out = new PrintStream(outFile);
            DefaultPrintVisitor printVisitor = new DefaultPrintVisitor(hier_out, theObj.getResult().getSamples());
            theObj.getResult().getRoot().topDownVisit(printVisitor);
            hier_out.close();
        }catch (Exception ex){
            outFile.delete();
            throw ex;
        }
    }
    
}
