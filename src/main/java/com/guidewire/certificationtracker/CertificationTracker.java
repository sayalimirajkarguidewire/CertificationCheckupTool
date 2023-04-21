package com.guidewire.certificationtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertificationTracker {
  // person x track x level x releases
  private Map<String, Map<String, Map<String, List<String>>>> aggregatorMap;
  private Map<String, Set<String>> releaseToUniqueUsers;
  private Map<String, List<String>> releaseToNonUniqueUsers;
  private TrackCertificationRules trackCertificationRules;
  private Map<String, String> emailToNameMap;
  private static List<String> RELEASE_ORDER = Arrays.asList("Aspen", "Banff", "Cortina", "Dobson", "Elysian", "Flaine",
    "Garmisch");

  public CertificationTracker(String inputPath) throws Exception {
    this.aggregatorMap = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    InputStream inputStream = CertificationTrackerGUI.class.getResourceAsStream("/trackCertificationRules.json");
    String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    this.trackCertificationRules = objectMapper.readValue(text, TrackCertificationRules.class);
    this.emailToNameMap = new HashMap<>();
    this.releaseToUniqueUsers = new HashMap<>();
    this.releaseToNonUniqueUsers = new HashMap<>();
    CSVUtil.readCsvAsList(inputPath, true).stream()
      .forEach(row -> {
        try {
          String email = row[1];
          String courseName = row[5];
          String[] courseNameParts = courseName.split("-�|-");
          String level = courseNameParts[0].trim();
          String track = courseNameParts[1].trim();
          String release = courseNameParts[2].trim();
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
    Map<String, Map<String, List<String>>> personMap = aggregatorMap.get(email);
    personMap.entrySet()
            .stream()
            .sorted(Comparator.comparingInt(c -> c.getKey().equals("InsuranceSuite Developer")
                    || c.getKey().equals("InsuranceSuite Analyst") ? 0 : 1))
            .forEach(personEntry -> {
      String track = personEntry.getKey();
      Map<String, List<String>> intermediate = personEntry.getValue();
      outputCurrentCertifications(track, intermediate, outputBuilder);
      String linkedAssociateTrack = trackCertificationRules.rules.get(track).linkedAssociateTrack;
      Map<String, String> linkedAssociateTrackLevelMap = linkedAssociateTrack != null
              ? trackCertificationRules.rules.get(linkedAssociateTrack).associateMap
              : Collections.emptyMap();
      if (intermediate.containsKey("Guidewire Certified Associate")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Associate", track,
                trackCertificationRules.rules.get(track).associateMap,
                linkedAssociateTrackLevelMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      }
      if (intermediate.containsKey("Guidewire Certified Ace")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Ace", track,
                trackCertificationRules.rules.get(track).otherMap,
                linkedAssociateTrackLevelMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      } else if (intermediate.containsKey("Guidewire Certified Specialist")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Specialist", track,
                trackCertificationRules.rules.get(track).otherMap,
                linkedAssociateTrackLevelMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      } else if (intermediate.containsKey("Guidewire Certified Professional")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Professional", track,
                trackCertificationRules.rules.get(track).otherMap,
                linkedAssociateTrackLevelMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      }
    });
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
    List<String> pendingReleases = getPendingReleases(getMostRecentRelease(releases));
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
