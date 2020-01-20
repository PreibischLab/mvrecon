package net.preibisch.mvrecon.fiji.plugin;

import java.util.List;

import mpicbg.spim.data.sequence.ViewDescription;
import net.imglib2.Interval;
import net.preibisch.distribution.algorithm.clustering.scripting.TaskType;
import net.preibisch.distribution.headless.Clustering;
import net.preibisch.distribution.tools.helpers.IOHelpers;
import net.preibisch.mvrecon.fiji.plugin.fusion.FusionGUI;
import net.preibisch.mvrecon.fiji.spimdata.boundingbox.BoundingBox;
import net.preibisch.mvrecon.process.interestpointregistration.pairwise.constellation.grouping.Group;

public class MVreconCustering {

	public static void run(FusionGUI fusion) {
		TaskType task ;
		if (fusion.getNonRigidParameters().isActive())
			task = TaskType.NON_RIGID;
		else
			task = TaskType.FUSION;
		String xml = IOHelpers.getXML(fusion.getSpimData().getBasePath().getAbsolutePath());
		System.out.println("XML: "+xml);
		double downsampling = fusion.getDownsampling();
		System.out.println("downsampling: "+downsampling);
		BoundingBox interval = (BoundingBox) fusion.getBoundingBox();
		
		System.out.println("interval: "+interval.toString());
		List<Group<ViewDescription>> groups = fusion.getFusionGroups();
		
		Clustering.run(task,xml,downsampling,interval,groups);
		
		
		
	}

}
