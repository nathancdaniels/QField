/***************************************************************************
  geometryutils.cpp - GeometryUtils

 ---------------------
 begin                : 05.03.2020
 copyright            : (C) 2020 by Denis Rouzaud
 email                : denis@opengis.ch
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#include "geometryutils.h"
#include "rubberbandmodel.h"

#include <qgslinestring.h>
#include <qgspolygon.h>
#include <qgsproject.h>
#include <qgsvectorlayer.h>

GeometryUtils::GeometryUtils( QObject *parent )
  : QObject( parent )
{
}


QgsGeometry GeometryUtils::polygonFromRubberband( RubberbandModel *rubberBandModel, const QgsCoordinateReferenceSystem &crs )
{
  QgsPointSequence ring = rubberBandModel->pointSequence( crs, QgsWkbTypes::Point, true );
  QgsLineString ext( ring );
  std::unique_ptr<QgsPolygon> polygon = std::make_unique<QgsPolygon>();
  polygon->setExteriorRing( ext.clone() );
  QgsGeometry g( std::move( polygon ) );
  return g;
}

GeometryUtils::GeometryOperationResult GeometryUtils::reshapeFromRubberband( QgsVectorLayer *layer, QgsFeatureId fid, RubberbandModel *rubberBandModel )
{
  QgsFeature feature = layer->getFeature( fid );
  QgsGeometry geom = feature.geometry();
  if ( geom.isNull() || ( QgsWkbTypes::geometryType( geom.wkbType() ) != QgsWkbTypes::LineGeometry && QgsWkbTypes::geometryType( geom.wkbType() ) != QgsWkbTypes::PolygonGeometry ) )
    return GeometryUtils::GeometryOperationResult::InvalidBaseGeometry;

  QgsPointSequence points = rubberBandModel->pointSequence( layer->crs(), QgsWkbTypes::Point, false );
  QgsLineString reshapeLineString( points );

  GeometryUtils::GeometryOperationResult reshapeReturn = static_cast<GeometryUtils::GeometryOperationResult>( geom.reshapeGeometry( reshapeLineString ) );
  if ( reshapeReturn == GeometryUtils::GeometryOperationResult::Success )
  {
    //avoid intersections on polygon layers
    if ( layer->geometryType() == QgsWkbTypes::PolygonGeometry )
    {
      QList<QgsVectorLayer *> avoidIntersectionsLayers;
      switch ( QgsProject::instance()->avoidIntersectionsMode() )
      {
        case QgsProject::AvoidIntersectionsMode::AvoidIntersectionsCurrentLayer:
          avoidIntersectionsLayers.append( layer );
          break;
        case QgsProject::AvoidIntersectionsMode::AvoidIntersectionsLayers:
          avoidIntersectionsLayers = QgsProject::instance()->avoidIntersectionsLayers();
          break;
        case QgsProject::AvoidIntersectionsMode::AllowIntersections:
          break;
      }
      if ( !avoidIntersectionsLayers.isEmpty() )
      {
        QHash<QgsVectorLayer *, QSet<QgsFeatureId>> ignoredFeature;
        ignoredFeature.insert( layer, QSet<QgsFeatureId>() << fid );
        geom.avoidIntersections( avoidIntersectionsLayers, ignoredFeature );
      }

      if ( geom.isEmpty() ) //intersection removal might have removed the whole geometry
      {
        return GeometryUtils::GeometryOperationResult::NothingHappened;
      }
    }
    layer->changeGeometry( fid, geom );

    // Add topological points
    if ( QgsProject::instance()->topologicalEditing() )
    {
      layer->addTopologicalPoints( geom );
    }
  }

  return reshapeReturn;
}

GeometryUtils::GeometryOperationResult GeometryUtils::addRingFromRubberband( QgsVectorLayer *layer, QgsFeatureId fid, RubberbandModel *rubberBandModel )
{
  QgsPointSequence ring = rubberBandModel->pointSequence( layer->crs(), layer->wkbType(), true );

  //Try to fix invalid geometries, useful when being passed on a freehand digitized ring
  QgsGeometry geometry( new QgsLineString( ring ) );
  if ( !geometry.isNull() )
  {
    QVector<QgsGeometry::Error> errors;
    geometry = geometry.makeValid();
    if ( !geometry.isNull() )
      static_cast<QgsLineString *>( geometry.get() )->points( ring );
  }
  return static_cast<GeometryUtils::GeometryOperationResult>( layer->addRing( ring, &fid ) );
}

GeometryUtils::GeometryOperationResult GeometryUtils::splitFeatureFromRubberband( QgsVectorLayer *layer, RubberbandModel *rubberBandModel )
{
  QgsPointSequence line = rubberBandModel->pointSequence( layer->crs(), QgsWkbTypes::Point, false );
  return static_cast<GeometryUtils::GeometryOperationResult>( layer->splitFeatures( line, true ) );
}

QgsPoint GeometryUtils::coordinateToPoint( const QGeoCoordinate &coor )
{
  return QgsPoint( coor.longitude(), coor.latitude(), coor.altitude() );
}
