/*-
 * #%L
 * Software for the reconstruction of multi-view microscopic acquisitions
 * like Selective Plane Illumination Microscopy (SPIM) Data.
 * %%
 * Copyright (C) 2012 - 2017 Multiview Reconstruction developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package net.preibisch.mvrecon.fiji.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.registration.ViewTransformAffine;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.VoxelDimensions;
import mpicbg.spim.io.IOFunctions;
import net.imglib2.Dimensions;
import net.imglib2.RealPoint;
import net.imglib2.img.list.ListImg;
import net.imglib2.img.list.ListImgFactory;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.util.Util;
import net.preibisch.mvrecon.fiji.ImgLib2Temp.Pair;
import net.preibisch.mvrecon.fiji.ImgLib2Temp.ValuePair;
import net.preibisch.mvrecon.fiji.plugin.apply.ModelLink;
import net.preibisch.mvrecon.fiji.spimdata.SpimData2;
import net.preibisch.mvrecon.fiji.spimdata.ViewSetupUtils;
import net.preibisch.mvrecon.vecmath.Transform3D;

public class ApplyTransformationCore 
{

	public static final void locationForViewDescription(
			final int[] l,
			final ViewDescription vd,
			final HashMap< TimePoint, Integer > mapT,
			final HashMap< Channel, Integer > mapC,
			final HashMap< Illumination, Integer > mapI,
			final HashMap< Angle, Integer > mapA )
	{
		final TimePoint t = vd.getTimePoint();
		final Channel c = vd.getViewSetup().getChannel();
		final Illumination i = vd.getViewSetup().getIllumination();
		final Angle a = vd.getViewSetup().getAngle();

		l[ 0 ] = mapA.get( a );
		l[ 1 ] = mapI.get( i );
		l[ 2 ] = mapC.get( c );
		l[ 3 ] = mapT.get( t );
	}

	public static final int numEntries( final ListImg< ModelLink > img )
	{
		int numEntries = 0;

		for ( final ModelLink l : img )
			if ( l != null )
				++numEntries;

		return numEntries;
	}

	public static final ListImg< ModelLink > createTable(
			final SpimData data,
			final List< ViewId > viewIds,
			final boolean sameModelTimePoints,
			final boolean sameModelChannels,
			final boolean sameModelIlluminations,
			final boolean sameModelAngles,
			final HashMap< TimePoint, Integer > mapT,
			final HashMap< Channel, Integer > mapC,
			final HashMap< Illumination, Integer > mapI,
			final HashMap< Angle, Integer > mapA )
	{
		final List< TimePoint > ts = SpimData2.getAllTimePointsSorted( data, viewIds );
		final List< Channel > cs = SpimData2.getAllChannelsSorted( data, viewIds );
		final List< Illumination > is = SpimData2.getAllIlluminationsSorted( data, viewIds );
		final List< Angle > as = SpimData2.getAllAnglesSorted( data, viewIds );

		final int nT = sameModelTimePoints ? 1 : ts.size();
		for ( int i = 0; i < ts.size(); ++i )
			mapT.put( ts.get( i ), sameModelTimePoints ? 0 : i );

		final int nC = sameModelChannels ? 1 : cs.size();
		for ( int i = 0; i < cs.size(); ++i )
			mapC.put( cs.get( i ), sameModelChannels ? 0 : i );

		final int nI = sameModelIlluminations ? 1 : is.size();
		for ( int i = 0; i < is.size(); ++i )
			mapI.put( is.get( i ), sameModelIlluminations ? 0 : i );

		final int nA = sameModelAngles ? 1 : as.size();
		for ( int i = 0; i < as.size(); ++i )
			mapA.put( as.get( i ), sameModelAngles ? 0 : i );

		// iterate first angles, then illums, then channels, then timepoints
		return new ListImgFactory< ModelLink >().create( new long[]{ nA, nI, nC, nT }, new ModelLink( null ) );
	}


	public static void preConcatenateTransform( final SpimData spimData, final ViewId viewId, final AffineTransform3D model, final String name )
	{
		final ViewRegistrations viewRegistrations = spimData.getViewRegistrations();

		// update the view registration
		final ViewRegistration vr = viewRegistrations.getViewRegistration( viewId );
		final ViewTransform vt = new ViewTransformAffine( name, model );
		vr.preconcatenateTransform( vt );
	}
	
	public static void setModelToCalibration( final SpimData spimData, final ViewId viewId, final double minResolution )
	{
		setModelToIdentity( spimData, viewId );
		
		final ViewRegistrations viewRegistrations = spimData.getViewRegistrations();
		final ViewRegistration r = viewRegistrations.getViewRegistration( viewId );
		
		final ViewDescription viewDescription = spimData.getSequenceDescription().getViewDescription( 
				viewId.getTimePointId(), viewId.getViewSetupId() );

		VoxelDimensions voxelSize = ViewSetupUtils.getVoxelSizeOrLoad( viewDescription.getViewSetup(), viewDescription.getTimePoint(), spimData.getSequenceDescription().getImgLoader() );
		final double calX = voxelSize.dimension( 0 ) / minResolution;
		final double calY = voxelSize.dimension( 1 ) / minResolution;
		final double calZ = voxelSize.dimension( 2 ) / minResolution;
		
		final AffineTransform3D m = new AffineTransform3D();
		m.set( calX, 0.0f, 0.0f, 0.0f, 
			   0.0f, calY, 0.0f, 0.0f,
			   0.0f, 0.0f, calZ, 0.0f );
		final ViewTransform vt = new ViewTransformAffine( "calibration", m );
		r.preconcatenateTransform( vt );
	}

	public static void setModelToIdentity( final SpimData spimData, final ViewId viewId )
	{
		final ViewRegistrations viewRegistrations = spimData.getViewRegistrations();
		final ViewRegistration r = viewRegistrations.getViewRegistration( viewId );
		r.identity();
	}

	public static void applyAxis( final SpimData data )
	{
		ViewRegistrations viewRegistrations = data.getViewRegistrations();
		for ( final ViewDescription vd : data.getSequenceDescription().getViewDescriptions().values() )
		{
			if ( vd.isPresent() )
			{
				final Angle a = vd.getViewSetup().getAngle();
				
				if ( a.hasRotation() )
				{
					final ViewRegistration vr = viewRegistrations.getViewRegistration( vd );

					final Dimensions dim = vd.getViewSetup().getSize();

					AffineTransform3D model = new AffineTransform3D();
					model.set(
							1, 0, 0, -dim.dimension( 0 )/2,
							0, 1, 0, -dim.dimension( 1 )/2,
							0, 0, 1, -dim.dimension( 2 )/2 );
					ViewTransform vt = new ViewTransformAffine( "Center view", model );
					vr.preconcatenateTransform( vt );

					final double[] tmp = new double[ 16 ];
					final double[] axis = a.getRotationAxis();
					final double degrees = -a.getRotationAngleDegrees();
					final Transform3D t = new Transform3D();
					final String d;

					if ( axis[ 0 ] == 1 && axis[ 1 ] == 0 && axis[ 2 ] == 0 )
					{
						t.rotX( Math.toRadians( degrees ) );
						d = "Rotation around x-axis by " + (-degrees) + " degrees";
					}
					else if ( axis[ 0 ] == 0 && axis[ 1 ] == 1 && axis[ 2 ] == 0 )
					{
						t.rotY( Math.toRadians( degrees ) );
						d = "Rotation around y-axis by " + (-degrees) + " degrees";
					}
					else if ( axis[ 0 ] == 1 && axis[ 0 ] == 0 && axis[ 2 ] == 1 )
					{
						t.rotZ( Math.toRadians( degrees ) );
						d = "Rotation around z-axis by " + (-degrees) + " degrees";
					}
					else
					{
						IOFunctions.println( "Arbitrary rotation axis not supported yet." );
						continue;
					}

					t.get( tmp );

					model = new AffineTransform3D();
					model.set( tmp[ 0 ], tmp[ 1 ], tmp[ 2 ], tmp[ 3 ],
							   tmp[ 4 ], tmp[ 5 ], tmp[ 6 ], tmp[ 7 ],
							   tmp[ 8 ], tmp[ 9 ], tmp[ 10 ], tmp[ 11 ] );

					vt = new ViewTransformAffine( d, model );
					vr.preconcatenateTransform( vt );
					vr.updateModel();
				}
			}
		}
	}

	public static AffineTransform3D rotationAngleAxis(double angle, double[] axis)
	{
		double[] u = toUnit( axis );
		double[] a = new double[12];

		// https://en.wikipedia.org/wiki/Rotation_formalisms_in_three_dimensions
		a[0] = (1.0 - Math.cos( angle )) * Math.pow( u[0], 2 ) + Math.cos( angle );
		a[1] = (1.0 - Math.cos( angle )) * u[0] * u[1] - u[2] * Math.sin( angle );
		a[2] = (1.0 - Math.cos( angle )) * u[0] * u[2] + u[1] * Math.sin( angle );

		a[4] = (1.0 - Math.cos( angle )) * u[0] * u[1] + u[2] * Math.sin( angle );
		a[5] = (1.0 - Math.cos( angle )) * Math.pow( u[1], 2 ) + Math.cos( angle );
		a[6] = (1.0 - Math.cos( angle )) * u[1] * u[2] - u[0] * Math.sin( angle );

		a[8] = (1.0 - Math.cos( angle )) * u[0] * u[2] - u[1] * Math.sin( angle );
		a[9] = (1.0 - Math.cos( angle )) * u[1] * u[2] + u[0] * Math.sin( angle );
		a[10] = (1.0 - Math.cos( angle )) * Math.pow( u[2], 2 ) + Math.cos( angle );

		final AffineTransform3D res = new AffineTransform3D();
		res.set( a );
		return res.copy();
		
	}

	public static double[] toUnit(double[] vec)
	{
		double len = 0;
		for (int i=0; i<vec.length; i++)
			len += Math.pow( vec[i], 2 );
		len = Math.sqrt( len );
		final double[] unit = new double[vec.length];
		for (int i=0; i<vec.length; i++)
			unit[i] = vec[i] / len;
		return unit;
	}

	public static void applyAxisGrouped(final SpimData data)
	{
		applyAxisGrouped( data, true );
	}

	public static void applyAxisGrouped(final SpimData data, boolean moveToOrigin)
	{
		final Map< Angle, Collection< Pair< Dimensions, AffineGet > > > viewsGroupedByAngle = new HashMap<>();
		final Map< Angle, double[] > angleCentersOfMasss = new HashMap<>();
		final ViewRegistrations viewRegistrations = data.getViewRegistrations();

		// Group by Angle
		for ( final ViewDescription vd : data.getSequenceDescription().getViewDescriptions().values() )
		{
			if ( vd.isPresent() )
			{
				final Angle a = vd.getViewSetup().getAngle();
				final ViewRegistration vr = viewRegistrations.getViewRegistration( vd );
				final Dimensions dim = vd.getViewSetup().getSize();

				if (!viewsGroupedByAngle.containsKey( a ))
					viewsGroupedByAngle.put( a, new ArrayList<>() );

				viewsGroupedByAngle.get( a ).add( new ValuePair<>( dim, vr.getModel() ) );
			}
		}

		viewsGroupedByAngle.forEach( (a, v) -> {angleCentersOfMasss.put( a, getCenterOfMass( v ));});

		for ( final ViewDescription vd : data.getSequenceDescription().getViewDescriptions().values() )
		{
			if ( vd.isPresent() )
			{
				final Angle a = vd.getViewSetup().getAngle();
				final ViewRegistration vr = viewRegistrations.getViewRegistration( vd );
				final double[] axis = a.getRotationAxis();
				final double degrees = -a.getRotationAngleDegrees();

				// no axis defined
				if ( axis == null || Util.distance( new RealPoint( axis ), new RealPoint( new double[axis.length] ) ) < Double.MIN_VALUE )
					continue;
				final double[] u = toUnit( axis );
				double angle = Math.toRadians( degrees );

				// get center
				double[] center = angleCentersOfMasss.get( a );
				if (center == null)
					continue;

				// center translation
				AffineTransform3D translateToCenter = new AffineTransform3D();
				translateToCenter.set(
						1, 0, 0, center[0],
						0, 1, 0, center[1],
						0, 0, 1, center[2] );

				// (1) move CoM to origin
				final AffineTransform3D res = new AffineTransform3D();
				res.preConcatenate( translateToCenter.inverse() );

				// (2) rotate
				AffineTransform3D rot = rotationAngleAxis( angle, u );
				res.preConcatenate( rot );

				// (3) move back to old CoM (usless we want to stay at origin)
				if (!moveToOrigin)
					res.preConcatenate( translateToCenter );

				final String desc = "Rotation around axis " + Util.printCoordinates( u ) + " by " + (-degrees) + " degrees";
				final ViewTransform vt = new ViewTransformAffine( desc, res );
				vr.preconcatenateTransform( vt );
				vr.updateModel();
			}
		}
	}

	public static double[] getCenterOfMass(Collection< Pair< Dimensions, AffineGet > > views)
	{
		if (views.size() < 1)
			return null;

		final int nDims = views.iterator().next().getA().numDimensions();
		final double[] center = new double[nDims];
		final double[] vertex = new double[nDims];
		final double[] vertexTransformed = new double[nDims];
		int count = 0;

		for (final Pair< Dimensions, AffineGet > view : views)
		{
			for (int i = 0; i< (int) Math.pow( 2, nDims ); i++)
			{

				// get i'th vertex of (0, 0, ...) - (dim_0, dim_1, ...)
				int ii = i;
				for (int j = 0; j<nDims; j++)
				{
					vertex[j] = ii%2 == 0 ? 0 : view.getA().dimension( j );
					ii /= 2;
				}

				view.getB().apply( vertex, vertexTransformed );

				for (int j = 0; j<nDims; j++)
					center[j] += vertexTransformed[j];

				count++;
			}
		}

		for (int i = 0; i<nDims; i++)
			center[i] /= count;

		return center;
	}

}
