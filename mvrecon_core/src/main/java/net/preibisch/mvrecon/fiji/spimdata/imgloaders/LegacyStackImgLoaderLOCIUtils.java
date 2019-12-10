package net.preibisch.mvrecon.fiji.spimdata.imgloaders;

import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.IFormatReader;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

public class LegacyStackImgLoaderLOCIUtils {
	public static final float getFloatValue(final byte[] b, final int i, final boolean isLittleEndian) {
		if (isLittleEndian)
			return Float.intBitsToFloat(
					((b[i + 3] & 0xff) << 24) + ((b[i + 2] & 0xff) << 16) + ((b[i + 1] & 0xff) << 8) + (b[i] & 0xff));
		else
			return Float.intBitsToFloat(
					((b[i] & 0xff) << 24) + ((b[i + 1] & 0xff) << 16) + ((b[i + 2] & 0xff) << 8) + (b[i + 3] & 0xff));
	}

	public static final int getIntValue(final byte[] b, final int i, final boolean isLittleEndian) {
		if (isLittleEndian)
			return (((b[i + 3] & 0xff) << 24) + ((b[i + 2] & 0xff) << 16) + ((b[i + 1] & 0xff) << 8) + (b[i] & 0xff));
		else
			return (((b[i] & 0xff) << 24) + ((b[i + 1] & 0xff) << 16) + ((b[i + 2] & 0xff) << 8) + (b[i + 3] & 0xff));
	}

	public static final short getShortValue(final byte[] b, final int i, final boolean isLittleEndian) {
		return (short) getShortValueInt(b, i, isLittleEndian);
	}

	public static final int getShortValueInt(final byte[] b, final int i, final boolean isLittleEndian) {
		if (isLittleEndian)
			return ((((b[i + 1] & 0xff) << 8)) + (b[i] & 0xff));
		else
			return ((((b[i] & 0xff) << 8)) + (b[i + 1] & 0xff));
	}

	protected static String checkPath(String path) {
		if (path.length() > 1) {
			path = path.replace('\\', '/');
			if (!path.endsWith("/"))
				path = path + "/";
		}

		return path;
	}

	public static boolean createOMEXMLMetadata(final IFormatReader r) {
		try {
			final ServiceFactory serviceFactory = new ServiceFactory();
			final OMEXMLService service = serviceFactory.getInstance(OMEXMLService.class);
			final IMetadata omexmlMeta = service.createOMEXMLMetadata();
			r.setMetadataStore(omexmlMeta);
		} catch (final ServiceException e) {
			e.printStackTrace();
			return false;
		} catch (final DependencyException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
}
