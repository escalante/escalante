/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.escalante.artifact.maven

import org.apache.maven.settings.Settings
import collection.JavaConversions._
import org.apache.maven.settings.{Repository => MavenRepository}
import org.apache.maven.settings.{RepositoryPolicy => MavenRepositoryPolicy}
import org.sonatype.aether.repository.{RepositoryPolicy, RemoteRepository}
import collection.mutable.ListBuffer
import org.sonatype.aether.util.repository.DefaultMirrorSelector
import java.io.File
import org.apache.maven.settings.building.{DefaultSettingsBuilderFactory, DefaultSettingsBuildingRequest}

/**
 * // TODO: Document this
 * @author Galder Zamarreño
 * @since // TODO
 */
class MavenSettings {

  import MavenSettings._

  val useMavenCentral = true

  val settings: Settings = buildDefaultSettings

  def getRemoteRepositories: Seq[RemoteRepository] = {
    // Disable repositories if working offline
    if (settings.isOffline)
      return Nil

    val actives = settings.getActiveProfiles

    val enhancedRepos = new ListBuffer[RemoteRepository]()

    asScalaIterator(settings.getProfilesAsMap.iterator).foreach {
      case (profileName, profile) =>
        val activation = profile.getActivation
        if (actives.contains(profileName) || (activation != null
            && activation.isActiveByDefault)) {
          asScalaIterator(profile.getRepositories.iterator()).foreach(
            repo => enhancedRepos += asRemoteRepository(repo))
        }
    }

    // Repositories from other model pom files could be loaded here...

    if (useMavenCentral)
      enhancedRepos += MAVEN_CENTRAL

    // Add Typesafe repo
    enhancedRepos += TYPESAFE

    // Add Sonatype, which holds latest Lift's jars
    enhancedRepos += SONATYPE_SNAPSHOT
    enhancedRepos += SONATYPE


    if (settings.getMirrors.size() == 0)
      return enhancedRepos ++ List()

    // Use mirrors if any to do the mirroring stuff
    val dms = new DefaultMirrorSelector()
    // Fill in mirrors
    asScalaIterator(settings.getMirrors.iterator()).foreach {
      mirror =>
      // Repository manager flag is set to false
      // Maven does not support specifying it in the settings.xml
        dms.add(mirror.getId, mirror.getUrl, mirror.getLayout,
          false, mirror.getMirrorOf, mirror.getMirrorOfLayouts)
    }

    val mirrors = enhancedRepos.map {
      repo =>
        val mirror = dms.getMirror(repo)
        if (mirror != null) mirror else repo
    }

    mirrors ++ List()
  }

}

object MavenSettings {

  // TODO: Externalize repositories to artifact module configuration

  private val MAVEN_CENTRAL = new RemoteRepository(
    "central", "default", "http://repo1.maven.org/maven2")

  private val TYPESAFE = new RemoteRepository(
    "typesafe", "default", "http://repo.typesafe.com/typesafe/maven-releases")

  private val SONATYPE_SNAPSHOT = new RemoteRepository(
    "sonatype-snapshot", "default", "http://oss.sonatype.org/content/repositories/snapshots")

  private val SONATYPE = new RemoteRepository(
    "sonatype", "default", "http://oss.sonatype.org/content/repositories/releases")

  private val ALT_USER_SETTINGS_XML_LOCATION = "org.apache.maven.user-settings"

  private val ALT_GLOBAL_SETTINGS_XML_LOCATION = "org.apache.maven.global-settings"

  private val DEFAULT_USER_SETTINGS_PATH = SecurityActions.getSystemProperty("user.home")
      .concat("/.m2/settings.xml")

  private val ALT_LOCAL_REPOSITORY_LOCATION = "maven.repo.local"

  private val DEFAULT_REPOSITORY_PATH = SecurityActions
      .getSystemProperty("user.home").concat("/.m2/repository")

  private val ALT_MAVEN_OFFLINE = "org.apache.maven.offline"

  private def buildDefaultSettings: Settings = {
    val request = new DefaultSettingsBuildingRequest()
    val altUserSettings = SecurityActions.getSystemProperty(
      ALT_USER_SETTINGS_XML_LOCATION)
    val altGlobalSettings = SecurityActions.getSystemProperty(
      ALT_GLOBAL_SETTINGS_XML_LOCATION)

    request.setUserSettingsFile(new File(DEFAULT_USER_SETTINGS_PATH))

    // set alternate files
    if (altUserSettings != null && altUserSettings.length() > 0)
      request.setUserSettingsFile(new File(altUserSettings))

    if (altGlobalSettings != null && altGlobalSettings.length() > 0)
      request.setGlobalSettingsFile(new File(altGlobalSettings))

    val result = new DefaultSettingsBuilderFactory().newInstance().build(request)

    val settings = result.getEffectiveSettings

    // enrich with local repository
    if (settings.getLocalRepository == null) {
      val altLocalRepo = SecurityActions.getSystemProperty(ALT_LOCAL_REPOSITORY_LOCATION)
      settings.setLocalRepository(DEFAULT_REPOSITORY_PATH)

      if (altLocalRepo != null && altLocalRepo.length() > 0)
        settings.setLocalRepository(altLocalRepo)
    }

    val goOffline = SecurityActions.getSystemProperty(ALT_MAVEN_OFFLINE)
    if (goOffline != null)
      settings.setOffline(goOffline.toBoolean)

    settings
  }

  private def asRemoteRepository(repository: MavenRepository): RemoteRepository = {
    new RemoteRepository().setId(repository.getId).setContentType(repository.getLayout)
        .setUrl(repository.getUrl).setPolicy(true, asRepositoryPolicy(repository.getSnapshots))
        .setPolicy(false, asRepositoryPolicy(repository.getReleases))
  }

  private def asRepositoryPolicy(policy: MavenRepositoryPolicy): RepositoryPolicy = {
    if (policy != null) {
      new RepositoryPolicy(policy.isEnabled,
        if (policy.getUpdatePolicy != null)
          policy.getUpdatePolicy
        else RepositoryPolicy.UPDATE_POLICY_DAILY,
        if (policy.getChecksumPolicy != null)
          policy.getChecksumPolicy
        else RepositoryPolicy.CHECKSUM_POLICY_WARN)
    } else {
      new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY,
        RepositoryPolicy.CHECKSUM_POLICY_WARN)
    }
  }

}
