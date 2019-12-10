package net.preibisch.mvrecon.fiji.plugin.util;

import bdv.export.ProgressWriter;
import bdv.export.ProgressWriterConsole;
import net.preibisch.mvrecon.fiji.plugin.resave.ProgressWriterIJ;

public class IJUtils {
	
	public static boolean printIJLog = true;
	
	private static ProgressWriterIJ progressWriterIJ = new ProgressWriterIJ();
	private static ProgressWriterConsole progressWriterConsole = new ProgressWriterConsole();
	public static ProgressWriter getProgressWriter()
	{
		if ( printIJLog )
			return progressWriterIJ;
		else
			return progressWriterConsole;
	}
}
