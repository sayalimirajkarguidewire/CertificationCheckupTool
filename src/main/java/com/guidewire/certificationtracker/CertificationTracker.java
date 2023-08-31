package com.guidewire.certificationtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertificationTracker {
  // person x track x level x releases
  private Map<String, Map<String, Map<String, List<String>>>> aggregatorMap;
  private Map<String, Set<String>> releaseToUniqueUsers;
  private Map<String, List<String>> releaseToNonUniqueUsers;
  private TrackCertificationRules trackCertificationRules;
  private Map<String, String> emailToNameMap;
  private Map<String, String> emailToManagerMap;
  private Map<String, String> emailToOrganizationMap;
  private static List<String> RELEASE_ORDER = Arrays.asList("Aspen", "Banff", "Cortina", "Dobson", "Elysian", "Flaine",
    "Garmisch", "Hakuba");

  public CertificationTracker(String inputPath) throws Exception {
    this.aggregatorMap = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    InputStream inputStream = CertificationTrackerGUI.class.getResourceAsStream("/trackCertificationRules.json");
    String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    this.trackCertificationRules = objectMapper.readValue(text, TrackCertificationRules.class);
    this.emailToNameMap = new HashMap<>();
    this.emailToManagerMap = new HashMap<>();
    this.emailToOrganizationMap = new HashMap<>();
    this.releaseToUniqueUsers = new HashMap<>();
    this.releaseToNonUniqueUsers = new HashMap<>();
    CSVUtil.readCsvAsList(inputPath, true).stream()
      .forEach(row -> {
        try {
          String email = row[1];
          String courseName = row[5];
          String[] courseNameParts = courseName.split("-�|-");
          String level = getAlphaNumericString(courseNameParts[0].trim());
          String track = getAlphaNumericString(courseNameParts[1].trim());
          String release = getAlphaNumericString(courseNameParts[2].trim());
          if (releaseToUniqueUsers.containsKey(release)) {
            releaseToUniqueUsers.get(release).add(email);
          } else {
            releaseToUniqueUsers.put(release, new HashSet<>());
            releaseToUniqueUsers.get(release).add(email);
          }
          if (releaseToNonUniqueUsers.containsKey(release)) {
            releaseToNonUniqueUsers.get(release).add(email);
          } else {
            releaseToNonUniqueUsers.put(release, new ArrayList<>());
            releaseToNonUniqueUsers.get(release).add(email);
          }
          emailToNameMap.put(email, row[0]);
          emailToOrganizationMap.put(email, row[2]);
          emailToManagerMap.put(email, row[27]);
          if (!aggregatorMap.containsKey(email)) {
            aggregatorMap.put(email, new HashMap<>());
          }
          if (!aggregatorMap.get(email).containsKey(track)) {
            aggregatorMap.get(email).put(track, new HashMap<>());
          }
          if (!aggregatorMap.get(email).get(track).containsKey(level)) {
            aggregatorMap.get(email).get(track).put(level, new ArrayList<>());
          }
          aggregatorMap.get(email).get(track).get(level).add(release);
          // System.out.println(RichStream.ofs(email, track, level, release).join(","));
        } catch (Exception e) {
          System.out.println(e.getStackTrace());
        }
      });
  }

  private String getAlphaNumericString(String input) {
    int startIndex = 0;
    for (int i = 0; i < input.length(); i++) {
      if (Character.isAlphabetic(input.charAt(i))) {
        startIndex = i;
        break;
      }
    }
    return input.substring(startIndex);
  }

  public void analyzeDataForAllUsers(String outputFilePath) throws Exception {
    List<String> outputLines = this.emailToNameMap.entrySet()
            .stream()
            .map(entry -> {
              try {
                String recommendations = this.getRecommendations(entry.getKey());
                return entry.getKey() + ",\"" + emailToOrganizationMap.get(entry.getKey()) + "\",\""
                        + emailToManagerMap.get(entry.getKey()) + "\"," + recommendations;
              } catch (Exception e) {
                System.out.println("Exception occurred : " + e.getStackTrace());
              }
              return "";
            })
            .collect(Collectors.toList());
    Files.write(Paths.get(outputFilePath), outputLines);
  }

  public static void main(String[] args) throws Exception {
    CertificationTracker certificationTracker = new CertificationTracker("/Users/smirajkar/Downloads/test_data.csv");

    //System.out.print(certificationTracker.getRecommendations("kalyan.chakravarthy@markel.com"));
    certificationTracker.getNumUniqueUsersByRelease();
    certificationTracker.getNumNonUniqueUsersByRelease();
  }

  public void getNumUniqueUsersByRelease() {
    releaseToUniqueUsers.entrySet()
            .stream()
            .sorted(Comparator.comparing(x -> x.getKey()))
            .forEach(entry ->
                    System.out.println(entry.getKey() + " " + entry.getValue().size()));
  }

  public void getNumNonUniqueUsersByRelease() {
    releaseToNonUniqueUsers.entrySet()
            .stream()
            .sorted(Comparator.comparing(x -> x.getKey()))
            .forEach(entry ->
                    System.out.println(entry.getKey() + " " + entry.getValue().size()));
  }

  // track x level x releases
  public String getRecommendations(String email) {
    StringBuilder outputBuilder = new StringBuilder();
    outputBuilder.append("<html><body>");
    if (!aggregatorMap.containsKey(email)) {
      return "No user found with ID : <b color=\"red\">" + email + "</b></body></html>";
    }
    Map<String, Map<String, List<String>>> personMap = aggregatorMap.get(email);
    AtomicBoolean hasValidLevel = new AtomicBoolean(false);
    personMap.entrySet()
            .stream()
            .sorted(Comparator.comparingInt(c -> c.getKey().equals("InsuranceSuite Developer")
                    || c.getKey().equals("InsuranceSuite Analyst") ? 0 : 1))
            .forEach(personEntry -> {
      String track = personEntry.getKey();
      Map<String, List<String>> intermediate = personEntry.getValue();
      outputCurrentCertifications(track, intermediate, outputBuilder);
      if (!trackCertificationRules.rules.containsKey(track)) {
        outputBuilder.append("Unknown Track : " + track + "<br>");
      } else {
        String linkedAssociateTrack = trackCertificationRules.rules.get(track).linkedAssociateTrack;
        Map<String, String> linkedAssociateTrackLevelMap = linkedAssociateTrack != null
                ? trackCertificationRules.rules.get(linkedAssociateTrack).associateMap
                : Collections.emptyMap();
        boolean isValidLevel = false;
        if (intermediate.containsKey("Guidewire Certified Associate")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Associate", track,
                  trackCertificationRules.rules.get(track).associateMap,
                  linkedAssociateTrackLevelMap,
                  trackCertificationRules.rules.get(track).preRequisiteMap);
          isValidLevel = true;
        }
        if (intermediate.containsKey("Guidewire Certified Ace")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Ace", track,
                  trackCertificationRules.rules.get(track).otherMap,
                  linkedAssociateTrackLevelMap,
                  trackCertificationRules.rules.get(track).preRequisiteMap);
          isValidLevel = true;
        } else if (intermediate.containsKey("Guidewire Certified Specialist")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Specialist", track,
                  trackCertificationRules.rules.get(track).otherMap,
                  linkedAssociateTrackLevelMap,
                  trackCertificationRules.rules.get(track).preRequisiteMap);
          isValidLevel = true;
        } else if (intermediate.containsKey("Guidewire Certified Professional")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Professional", track,
                  trackCertificationRules.rules.get(track).otherMap,
                  linkedAssociateTrackLevelMap,
                  trackCertificationRules.rules.get(track).preRequisiteMap);
          isValidLevel = true;
        }
        if (!isValidLevel) {
          outputBuilder.append("Unknown Level : " + Arrays.toString(intermediate.keySet().toArray()) + "<br>");
        }
        hasValidLevel.set(isValidLevel);
      }
    });
    if (hasValidLevel.get()) {
      outputBuilder.append("<br><br>To access courses for updating certifications log into Guidewire Education "
        + "account and go to My Learning → My Learning Paths → New Features Subscription<br><br>");
    }
    outputBuilder.append("</body></html>");
    return outputBuilder.toString();
  }

  private void outputCurrentCertifications(String track,
                                           Map<String, List<String>> intermediate, StringBuilder outputBuilder) {
    outputBuilder.append("<b>Current certifications for Track: " + track + "</b><br>");
    intermediate.entrySet().stream()
            .forEach(entry -> {
              if (entry.getKey().equals("Guidewire Certified Associate")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("<br>");
              }
              if (entry.getKey().equals("Guidewire Certified Ace")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("<br>");
              } else if (entry.getKey().equals("Guidewire Certified Specialist")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("<br>");
              } else if (entry.getKey().equals("Guidewire Certified Professional")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("<br>");
              }
            });
  }

  private void recommendInternal(Map<String, List<String>> intermediate, StringBuilder outputBuilder,
                                 String level, String track, Map<String, String> trackLevelMap,
                                 Map<String, String> linkedAssociateTrackLevelMap,
                                 Map<String, List<String>> preRequisiteMap) {
    List<String> releases = intermediate.get(level);
    String mostRecentRelease = getMostRecentRelease(releases);
    if (!isValidRelease(mostRecentRelease)) {
      outputBuilder.append("Unknown Release : " + mostRecentRelease + "<br>");
      return;
    }
    List<String> pendingReleases = getPendingReleases(mostRecentRelease);
    if (pendingReleases.size() == 0 ||
            pendingReleases.stream().noneMatch(release -> trackLevelMap.containsKey(release))) {
      outputBuilder.append("<b><i style=\"color:green;\">Your certification is already up-to-date!</i></b>");
    } else {
      outputBuilder.append("<b><i style=\"color:blue;\">Take the following courses to update your " + level + " certification:</i></b>");
      outputBuilder.append("<br>");

      outputBuilder.append(Stream.of(getUpdateMessage(pendingReleases, linkedAssociateTrackLevelMap, Collections.emptyMap()),
              getUpdateMessage(pendingReleases, trackLevelMap, preRequisiteMap))
              .filter(x -> !x.trim().isEmpty())
              .collect(Collectors.joining("<br>")));
    }
    outputBuilder.append("<br><br>");
  }

  private String getUpdateMessage(List<String> pendingReleases, Map<String, String> trackLevelMap,
                        Map<String, List<String>> preRequisiteMap) {
    return pendingReleases.stream()
            .filter(release -> trackLevelMap.containsKey(release))
            .flatMap(release -> {
              String parts[] = trackLevelMap.get(release).split(" : ");
              String course = parts[0];
              if (preRequisiteMap.containsKey(course)) {
                List<String> output = new ArrayList<>();
                output.addAll(preRequisiteMap.get(course));
                output.add(course);
                if (parts.length > 1) {
                  output.add("Recommended : " + parts[1]);
                }
                return output.stream();
              }
              return Stream.of(course);
            })
            .collect(Collectors.joining("<br>"));
  }

  private String getMostRecentRelease(List<String> inputReleases) {
    List<String> sortedReleases = inputReleases.stream()
      .sorted()
      .collect(Collectors.toList());
    return sortedReleases.get(sortedReleases.size() - 1);
  }

  private boolean isValidRelease(String inputRelease) {
    return RELEASE_ORDER.contains(inputRelease);
  }

  private List<String> getPendingReleases(String inputRelease) {
    int startIndex = RELEASE_ORDER.indexOf(inputRelease);
    if (startIndex == RELEASE_ORDER.size() - 1) {
      return Arrays.asList();
    }
    return RELEASE_ORDER.subList(startIndex + 1, RELEASE_ORDER.size());
  }

  public String getNameFromEmail(String email) {
    return emailToNameMap.get(email).split(" ")[0];
  }

  public String getFullNameFromEmail(String email) {
    return emailToNameMap.get(email);
  }
}
