/*
 * Copyright (c) 2014 Dufresne Management Consulting LLC.
 */
package models

import anorm._
import anorm.SqlParser._
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import AnormExtension._
import scala.language.postfixOps


// -----------------------------------------------------------------------------------------------------------
// Domain Case Classes
// -----------------------------------------------------------------------------------------------------------

/**
 * Case class for Artist
 *
 * Instances are created via the repository layer
 */
case class Artist(val id: Long, val status: Byte, val sourceId: Long, val name: String,
  val websiteUrl: Option[String])

/**
 * Case class for Venue
 *
 * Instances are created via the repository layer
 */
case class Venue(val id: Long, val status: Byte, val sourceId: Long, val name: String,
  val streetAddress: Option[String], val locationLat: Option[Float], val locationLng: Option[Float], val phoneNbr: Option[String],
  val websiteUrl: Option[String])

// -----------------------------------------------------------------------------------------------------------
// Repository Interfaces
// -----------------------------------------------------------------------------------------------------------
/**
 * The repository interface for Party
 *
 * This is used internally by Artists and Venues objects
 */
object Parties {

  val PENDING: Byte = 0
  val ACTIVE: Byte = 1

  val ARTIST_TYPEID: Long = 1 //* This is a hack, assuming: INSERT INTO TYPES VALUES (1, 'Artist');
  val VENUE_TYPEID: Long = 2 //* This is a hack, assuming: INSERT INTO TYPES VALUES (2, 'Venue');

  /**
   * Get all Party by type id and status
   * @TODO Add pagination
   */
  def getAll(typeId: Long, status: Byte = ACTIVE): List[(Long, Byte, Long, String, Option[String], Option[Float], Option[Float], Option[String], Option[String])] = DB.withConnection { implicit c =>
    SQL("""SELECT a.ID, a.STATUS, a.SOURCE_ID, a.NAME, a.STREET_ADDRESS, a.LOCATION_LAT, a.LOCATION_LNG, a.PHONE_NBR, a.WEBSITE_URL
		       FROM PARTIES a WHERE a.TYPE_ID = {typeId} AND a.STATUS = {status}
		       ORDER BY a.NAME""")
      .on('typeId -> typeId, 'status -> status)
      .as(get[Long]("ID") ~ get[Byte]("STATUS") ~ get[Long]("SOURCE_ID") ~ get[String]("NAME")
        ~ get[Option[String]]("STREET_ADDRESS") ~ get[Option[Float]]("LOCATION_LAT") ~ get[Option[Float]]("LOCATION_LNG") ~ get[Option[String]]("PHONE_NBR")
        ~ get[Option[String]]("WEBSITE_URL")
        map (flatten) *)
  }

  /**
   * Add Party while specifying primary key id
   */
  def add(id: Long, status: Byte, sourceId: Long, typeId: Long, name: String, streetAddress: Option[String], locationLat: Option[Float], locationLng: Option[Float], phoneNbr: Option[String], websiteUrl: Option[String]): Boolean = DB.withConnection { implicit c =>
    val result = SQL("""INSERT INTO PARTIES(ID, STATUS, SOURCE_ID, TYPE_ID, NAME, STREET_ADDRESS, LOCATION_LAT, LOCATION_LNG, PHONE_NBR, WEBSITE_URL) 
    			VALUES ({id}, {status}, {sourceId}, {typeId}, {name}, {streetAddress}, {locationLat}, {locationLng}, {phoneNbr}, {websiteUrl})""")
      .on('id -> id, 'status -> status, 'sourceId -> sourceId, 'typeId -> typeId, 'name -> name,
        'streetAddress -> streetAddress, 'locationLat -> locationLat, 'locationLng -> locationLng, 'phoneNbr -> phoneNbr,
        'websiteUrl -> websiteUrl).executeUpdate()

    result == 1
  }

  /**
   * Add Party, primary key specified by the auto increment
   */
  def add(status: Byte, sourceId: Long, typeId: Long, name: String, streetAddress: Option[String], locationLat: Option[Float], locationLng: Option[Float], phoneNbr: Option[String], websiteUrl: Option[String]): Option[Long] = DB.withConnection { implicit c =>
    SQL("""INSERT INTO PARTIES(STATUS, SOURCE_ID, TYPE_ID, NAME, STREET_ADDRESS, LOCATION_LAT, LOCATION_LNG, PHONE_NBR, WEBSITE_URL) 
    		VALUES ({status}, {sourceId}, {typeId}, {name}, {streetAddress}, {locationLat}, {locationLng}, {phoneNbr}, {websiteUrl})""")
      .on('status -> status, 'sourceId -> sourceId, 'typeId -> typeId, 'name -> name,
        'streetAddress -> streetAddress, 'locationLat -> locationLat, 'locationLng -> locationLng, 'phoneNbr -> phoneNbr,
        'websiteUrl -> websiteUrl).executeInsert()
  }

  /**
   * Get Party by primary key
   */
  def getById(id: Long): Option[(Byte, Long, Long, String, Option[String], Option[Float], Option[Float], Option[String], Option[String])] = DB.withConnection { implicit c =>
    SQL("SELECT a.STATUS, a.SOURCE_ID, a.TYPE_ID, a.NAME, a.STREET_ADDRESS, a.LOCATION_LAT, a.LOCATION_LNG, a.PHONE_NBR, a.WEBSITE_URL FROM PARTIES a WHERE a.ID = {id}")
      .on('id -> id)
      .as((get[Byte]("STATUS") ~ get[Long]("SOURCE_ID") ~ get[Long]("TYPE_ID") ~ get[String]("NAME")
        ~ get[Option[String]]("STREET_ADDRESS") ~ get[Option[Float]]("LOCATION_LAT") ~ get[Option[Float]]("LOCATION_LNG") ~ get[Option[String]]("PHONE_NBR")
        ~ get[Option[String]]("WEBSITE_URL")).singleOpt) map (flatten)
  }

  /**
   * Get Party by primary name and type id
   */
  def getByName(name: String, typeId: Long, status: Long): List[(Long, Byte, Long, String, Option[String], Option[Float], Option[Float], Option[String], Option[String])] = DB.withConnection { implicit c =>
    SQL("""SELECT a.ID, a.STATUS, a.SOURCE_ID, a.NAME, a.STREET_ADDRESS, LOCATION_LAT, LOCATION_LNG, a.PHONE_NBR, a.WEBSITE_URL
		       FROM PARTIES a WHERE a.TYPE_ID = {typeId} AND a.NAME LIKE {name} AND a.STATUS = {status}
		       ORDER BY a.NAME""")
      .on('typeId -> typeId, 'name -> ('%' + name + '%'), 'status -> status)
      .as(get[Long]("ID") ~ get[Byte]("STATUS") ~ get[Long]("SOURCE_ID") ~ get[String]("NAME")
        ~ get[Option[String]]("STREET_ADDRESS") ~ get[Option[Float]]("LOCATION_LAT") ~ get[Option[Float]]("LOCATION_LNG") ~ get[Option[String]]("PHONE_NBR")
        ~ get[Option[String]]("WEBSITE_URL")
        map (flatten) *)
  }

  /**
   * Get Party by location (longitude, latitude)
   */
  def getByLocation(locationLat: Float, locationLng: Float): Option[(Long, Byte, Long, Long, String, Option[String], Option[Float], Option[Float], Option[String], Option[String])] = DB.withConnection { implicit c =>
    SQL("SELECT a.STATUS, a.SOURCE_ID, a.TYPE_ID, a.NAME, a.STREET_ADDRESS, a.LOCATION_LAT, a.LOCATION_LNG, a.PHONE_NBR, a.WEBSITE_URL FROM PARTIES a WHERE a.ID = {id}")
      .on('locationLat -> locationLat, 'locationLng -> locationLng)
      .as((get[Long]("ID") ~ get[Byte]("STATUS") ~ get[Long]("SOURCE_ID") ~ get[Long]("TYPE_ID") ~ get[String]("NAME")
        ~ get[Option[String]]("STREET_ADDRESS") ~ get[Option[Float]]("LOCATION_LAT") ~ get[Option[Float]]("LOCATION_LNG") ~ get[Option[String]]("PHONE_NBR")
        ~ get[Option[String]]("WEBSITE_URL")).singleOpt) map (flatten)
  }

  /**
   * Delete by primary key and type ID
   */
  def delete(id: Long, typeId: Long): Boolean = DB.withConnection { implicit c =>
    SQL("DELETE FROM PARTIES a WHERE a.ID = {id} AND a.TYPE_ID = {typeId}").on('id -> id, 'typeId -> typeId).executeUpdate() == 1
  }

}

/**
 * The repository interface for Artist
 */
object Artists {

  /**
   * Get all Artists
   * @TODO Need to specify pagination parameters
   */
  def getAllArtists(): List[Artist] = {
    Parties.getAll(Parties.ARTIST_TYPEID, Parties.ACTIVE) map { case (id, status, sourceId, name, _, _, _, _, websiteUrl) => Artist(id, status, sourceId, name, websiteUrl) }
  }

  /**
   * Get Artists by name
   * @TODO Need to specify pagination parameters
   */
  def getArtistsByName(name: String): List[Artist] = {
    Parties.getByName(name, Parties.ARTIST_TYPEID, Parties.ACTIVE) map { case (id, status, sourceId, name, _, _, _, _, websiteUrl) => Artist(id, status, sourceId, name, websiteUrl) }
  }

  /**
   * Add Artist by specifying it's primary key.
   *
   * This method should be used only to load batch of artists.
   * @return None if the database insert fails
   */
  def addArtist(id: Long, status: Byte, sourceId: Long, name: String, websiteUrl: Option[String]): Option[Artist] = {
    if (Parties.add(id, status, sourceId, Parties.ARTIST_TYPEID, name, None, None, None, None, websiteUrl)) {
      Some(Artist(id, status, sourceId, name, websiteUrl))
    } else {
      None
    }
  }

  /**
   * Add Artist
   *
   * Use this method to add new artist into existing database
   * @return None if the database insert fails
   */
  def addArtist(status: Byte, sourceId: Long, name: String, websiteUrl: Option[String]): Option[Artist] = DB.withConnection { implicit c =>
    Parties.add(status, sourceId, Parties.ARTIST_TYPEID, name, None, None, None, None, websiteUrl) match {
      case Some(id) => Some(Artist(id, status, sourceId, name, websiteUrl))
      case None => None
    }
  }

  /**
   *  Get Artist by primary key
   *
   */
  def getArtistById(id: Long): Option[Artist] = {
    Parties.getById(id) match {
      case Some((status, sourceId, Parties.ARTIST_TYPEID, name, _, _, _, _, websiteUrl)) => Some(Artist(id, status, sourceId, name, websiteUrl))
      case _ => None
    }
  }

  /**
   * Delete Artist by primary key
   */
  def deleteArtist(artist: Artist): Boolean = {
    Parties.delete(artist.id, Parties.ARTIST_TYPEID)
  }
}

/**
 * The repository interface for Venue
 */
object Venues {

  /**
   * Get all Venues
   * @TODO Need to specify pagination parameters
   */
  def getAllVenues(): List[Venue] = {
    Parties.getAll(Parties.VENUE_TYPEID, Parties.ACTIVE) map { case (id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl) => Venue(id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl) }
  }

  /**
   * Get Venues by name
   * @TODO Need to specify pagination parameters
   */
  def getVenuesByName(name: String): List[Venue] = {
    Parties.getByName(name, Parties.VENUE_TYPEID, Parties.ACTIVE) map { case (id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl) => Venue(id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl) }
  }

  /**
   * Add Venue by specifying it's primary key.
   *
   * This method should be used only to load batch of Venues.
   * @return None if the database insert fails
   */
  def addVenue(id: Long, status: Byte, sourceId: Long, name: String, streetAddress: Option[String], locationLat: Option[Float], locationLng: Option[Float], phoneNbr: Option[String], websiteUrl: Option[String]): Option[Venue] = {
    if (Parties.add(id, status, sourceId, Parties.VENUE_TYPEID, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl)) {
      Some(Venue(id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl))
    } else {
      None
    }
  }

  /**
   * Add Venue
   *
   * Use this method to add new Venue into existing database
   * @return None if the database insert fails
   */
  def addVenue(status: Byte, sourceId: Long, name: String, streetAddress: Option[String], locationLat: Option[Float], locationLng: Option[Float], phoneNbr: Option[String], websiteUrl: Option[String]): Option[Venue] = DB.withConnection { implicit c =>
    Parties.add(status, sourceId, Parties.VENUE_TYPEID, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl) match {
      case Some(id) => Some(Venue(id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl))
      case None => None
    }
  }

  /**
   *  Get Venue by primary key
   *
   */
  def getVenueById(id: Long): Option[Venue] = {
    Parties.getById(id) match {
      case Some((status, sourceId, Parties.VENUE_TYPEID, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl)) => Some(Venue(id, status, sourceId, name, streetAddress, locationLat, locationLng, phoneNbr, websiteUrl))
      case _ => None
    }
  }

  /**
   * Delete Venue by primary key
   */
  def deleteVenue(venue: Venue): Boolean = {
    Parties.delete(venue.id, Parties.VENUE_TYPEID)
  }
}
