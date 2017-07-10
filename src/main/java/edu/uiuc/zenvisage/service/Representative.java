/**
 * 
 */
package edu.uiuc.zenvisage.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.io.FileWriter;
import java.io.BufferedWriter;
import edu.uiuc.zenvisage.service.utility.Normalization;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.uiuc.zenvisage.data.roaringdb.db.Database;
import edu.uiuc.zenvisage.data.roaringdb.executor.Executor;
import edu.uiuc.zenvisage.model.ZvQuery;
import edu.uiuc.zenvisage.service.cluster.Clustering;
import edu.uiuc.zenvisage.service.cluster.DummyCluster;
import edu.uiuc.zenvisage.service.cluster.RepresentativeTrend;
import edu.uiuc.zenvisage.service.distance.Distance;
import edu.uiuc.zenvisage.model.*;
import java.util.stream.IntStream;

/**
 *
 */
public class Representative extends Analysis {
	/**
	 * Cluster class for clustering.
	 */
	public Clustering cluster;

	/**
	 * @param executor
	 * @param inMemoryDatabase
	 * @param chartOutput
	 * @param distance
	 * @param normalization
	 * @param cluster
	 */
	public Representative(ChartOutputUtil chartOutput, 
			Distance distance, Normalization normalization, Clustering cluster, ZvQuery args) {
		super(chartOutput, distance, normalization, args);
		// TODO Auto-generated constructor stub
		this.cluster = cluster;
	}
	
	/**
	  * Return the indexes correspond to the top-k smallest in an array.
	  */
	public static int[] minKIndex(List<Double> array, int top_k) {
	    double[] min = new double[top_k];
	    int[] minIndex = new int[top_k];
	    Arrays.fill(min, Double.POSITIVE_INFINITY);
	    Arrays.fill(minIndex, -1);

	    top: for(int i = 0; i < array.size(); i++) {
	        for(int j = 0; j < top_k; j++) {
	            if(array.get(i) < min[j]) {
	                for(int x = top_k - 1; x > j; x--) {
	                    minIndex[x] = minIndex[x-1]; min[x] = min[x-1];
	                }
	                minIndex[j] = i; min[j] = array.get(i);
	                continue top;
	            }
	        }
	    }
	    return minIndex;
	}
	/* (non-Javadoc)
	 * @see analyze.Analysis#getAnalysis()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void compute(LinkedHashMap<String, LinkedHashMap<Float, Float>> output, double[][] normalizedgroups, ZvQuery args) throws JsonProcessingException, java.io.IOException {
		// TODO Auto-generated method stub
		ArrayList<String> mappings = new ArrayList<String>();
		for(String key : output.keySet()) {
			 mappings.add(key);
		}
		
//		System.out.print("normalizedgroups:");
//		System.out.println(normalizedgroups);
//		System.out.println(normalizedgroups.length);
//		System.out.println("normalizedgroups[0]:");
//		System.out.println(normalizedgroups[0].toString());
//		double eps = cluster.calculateEpsDistance(normalizedgroups, 2);
		double eps = 0;  //unused in calculateClusters
		@SuppressWarnings({ "rawtypes"})
		DummyCluster dc = cluster.calculateClusters(eps, 0, normalizedgroups);
		List clusters = dc.getClusters();		
		System.out.println("dc clusters:");
		System.out.println(clusters);
		//List<RepresentativeTrend> representativeTrends = cluster.computeRepresentativeTrends(clusters,mappings,normalizedgroups);
		//removeDuplicate(representativeTrends);
		//double[][] centers = cluster.getCenters(clusters);
		BufferedWriter bx = null;
		BufferedWriter by = null;
		if (args.getDownload()){
			 System.out.println("downloading RepresentativeTrends!");
//			 if (args.getDownloadX()){
//				 FileWriter fx = new FileWriter("representative_"+args.xAxis+".csv");
//				 bx = new BufferedWriter(fx);
//			 }
			 FileWriter fy = new FileWriter("representative_"+args.yAxis+".csv");
			 by = new BufferedWriter(fy);
		 }
		List<RepresentativeTrend> representativeTrends = new ArrayList<RepresentativeTrend>();
		List<List<Double>> clusteredTrends = new ArrayList();
	    for (int k = 0; k < clusters.size(); k++) {	    
	    		System.out.println("Cluster #"+ Integer.toString(k));
	    		RepresentativeTrend repTrend = new RepresentativeTrend();
	    		DoublePoint point = (DoublePoint) ((CentroidCluster<DoublePoint>) clusters.get(k)).getCenter();
	    		List<Double> clustTrend = new ArrayList();
			List<Double> clusterDist = new ArrayList();
	  	  	double[] p = point.getPoint();
	  	  	int min = 0;
	  	  	double mindist = distance.calculateDistance(p, normalizedgroups[0]);
//	  	    System.out.println("normalized groups size:"+ Integer.toString(normalizedgroups[l].length));
	  	  	for (int l = 1; l < normalizedgroups.length; l++) {
//		  	  	System.out.print("normalizedgroups[l].toString():");
//				//System.out.println(normalizedgroups[l].toString());
//				
//				System.out.print("[");
//				for (int z=0 ; z< normalizedgroups[l].length;z++){
//					System.out.print(normalizedgroups[l][z]);
//					System.out.print(',');
//				}
//				System.out.println("]");
				
	  	  		double d = distance.calculateDistance(p, normalizedgroups[l]);
	  	  		clusterDist.add(d);
	  	  		if (d < mindist ) {
	  	  			min = l;
	  	  		 	mindist = d;
	  	  		}
	  	  	}
	  	  	repTrend.setP(normalizedgroups[min]);
	  	  	repTrend.setKey(mappings.get(min));
	  	  	repTrend.setSimilarTrends(dc.getRealSizes()[k]);
	  	    representativeTrends.add(repTrend);
	  	    System.out.println("cluster size:"+Integer.toString(dc.getRealSizes()[k]));
	  	    System.out.print("clusterDist:");
			System.out.println(clusterDist);
			if (args.getDownload()){
				int [] minK_idx = minKIndex(clusterDist,dc.getRealSizes()[k]);
				System.out.print("minK_idx:");
				for (int i =0 ; i<minK_idx.length ; i++){
					String data_str = "";
					for (int n=0 ; n<normalizedgroups[minK_idx[i]].length;n++){
						data_str+=Double.toString(normalizedgroups[minK_idx[i]][n]) +",";
					}
					by.write(Integer.toString(k)+','+mappings.get(minK_idx[i])+','+ data_str.subSequence(0, data_str.length()-1)+"\n");
//					if (args.getDownloadX()){
//						bx.write(mappings.get(minK_idx[i])+','+ String.join(",", viz.xData)+"\n");
//					}
					 
					System.out.print('[');
					System.out.print(k+',');//Cluster label 
	//				System.out.print(Integer.toString(minK_idx[i])+',');
	//				System.out.print(clusterDist.get(minK_idx[i])); //min dist
					System.out.print(mappings.get(minK_idx[i])+',');
					System.out.print('(');
					for (int n=0 ; n<normalizedgroups[minK_idx[i]].length;n++){
						System.out.print(normalizedgroups[minK_idx[i]][n]);
					}
					System.out.print(')');
					System.out.println(']');
				}
			}
//			Collections.sort(clusterDist);
//			System.out.println("clusterDist:");
//			System.out.println(clusterDist);
//			Comparator<double []> comparator = Comparator.comparingDouble();
//	        IntStream.range(0,clusterDist.length)
//	                .mapToObj(i-> new Pair<>(i,clusterDist[i]))
//	                .sorted(comparator.reversed())
//	                .mapToInt(Pair::getFirst).toArray();
//			int[] sortedIdx = IntStream.range(0, clusterDist.size())
//			                .boxed()//.sorted()
//			                .sorted((i,j) ->  (clusterDist.get(i) - clusterDist.get(j)))
//			                .mapToInt(ele -> ele).toArray();
//			System.out.print("[");
//			for (int i =0 ; i<sortedIdx.length;i++){
//				System.out.print(sortedIdx[i]);
//				System.out.print(",");
//			}
//			System.out.println("]");
			 
//			System.out.print("mappings[sortedIdx]:");
//			System.out.println(mappings[sortedIdx]);
	    }
//	    System.out.println("mappings:");
//  	  	System.out.println(mappings);
//  	  	System.out.println(mappings.size());
  	  
	    Collections.sort(representativeTrends, new Comparator<RepresentativeTrend>() {

	        public int compare(RepresentativeTrend o1, RepresentativeTrend o2) {
	            if(o1.getSimilarTrends() < o2.getSimilarTrends())
	            	return 1;
	            else if (o1.getSimilarTrends() > o2.getSimilarTrends())
	            	return -1;
	            else
	            	return 0;
	        }
	    });	
	    if (args.getDownload()){
//	    		 if (args.getDownloadX()){bx.close();}
			 by.close();
	    }
	    
		chartOutput.chartOutput(representativeTrends,output,chartOutput.args,chartOutput.finalOutput);
	}
	/*
	private void removeDuplicate(List<RepresentativeTrend> representativeTrends) {
		int count = 0;
		for (int i = representativeTrends.size() - 1; i >= 0; i--) {
			if (count > 1)
				break;
			RepresentativeTrend src = representativeTrends.get(i);
			for (int j = i - 1; j >= 0; j--) {
				RepresentativeTrend tar = representativeTrends.get(j);
				double dist = distance.calculateDistance(src.getP(), tar.getP());
				if (dist < 1000) {
					count++;
					representativeTrends.remove(i);
					break;
				}
			}
		}
	}*/

}
