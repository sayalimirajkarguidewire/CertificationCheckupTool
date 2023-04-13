package com.guidewire.certificationtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CertificationTracker {
  // person x track x level x releases
  private Map<String, Map<String, Map<String, List<String>>>> aggregatorMap;
  private TrackCertificationRules trackCertificationRules;
  private static List<String> RELEASE_ORDER = Arrays.asList("Aspen", "Banff", "Cortina", "Dobson", "Elysian", "Flaine",
    "Garmisch");

  public CertificationTracker(String inputPath) throws Exception {
    this.aggregatorMap = new HashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    InputStream inputStream = CertificationTrackerGUI.class.getResourceAsStream("/trackCertificationRules.json");
    String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    this.trackCertificationRules = objectMapper.readValue(text, TrackCertificationRules.class);
    CSVUtil.readCsvAsList(inputPath, true).stream()
      .forEach(row -> {
        try {
          String email = row[1];
          String courseName = row[5];
          String[] courseNameParts = courseName.split("-�|-");
          String level = courseNameParts[0].trim();
          String track = courseNameParts[1].trim();
          String release = courseNameParts[2].trim();
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
    System.out.print(certificationTracker.getRecommendations("kalyan.chakravarthy@markel.com"));
  }

  // track x level x releases
  public String getRecommendations(String email) {
    StringBuilder outputBuilder = new StringBuilder();
    Map<String, Map<String, List<String>>> personMap = aggregatorMap.get(email);
    personMap.entrySet().forEach(personEntry -> {
      String track = personEntry.getKey();
      Map<String, List<String>> intermediate = personEntry.getValue();
      outputCurrentCertifications(track, intermediate, outputBuilder);
      if (intermediate.containsKey("Guidewire Certified Associate")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Associate", track,
                trackCertificationRules.rules.get(track).associateMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      }
      if (intermediate.containsKey("Guidewire Certified Ace")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Ace", track,
                trackCertificationRules.rules.get(track).otherMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      } else if (intermediate.containsKey("Guidewire Certified Specialist")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Specialist", track,
                trackCertificationRules.rules.get(track).otherMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      } else if (intermediate.containsKey("Guidewire Certified Professional")) {
        recommendInternal(intermediate, outputBuilder, "Guidewire Certified Professional", track,
                trackCertificationRules.rules.get(track).otherMap,
                trackCertificationRules.rules.get(track).preRequisiteMap);
      }
    });
    return outputBuilder.toString();
  }

  private void outputCurrentCertifications(String track,
                                           Map<String, List<String>> intermediate, StringBuilder outputBuilder) {
    outputBuilder.append("Current certifications for Track: " + track + "\n");
    intermediate.entrySet().stream()
            .forEach(entry -> {
              if (entry.getKey().equals("Guidewire Certified Associate")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("\n");
              }
              if (entry.getKey().equals("Guidewire Certified Ace")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("\n");
              } else if (entry.getKey().equals("Guidewire Certified Specialist")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("\n");
              } else if (entry.getKey().equals("Guidewire Certified Professional")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                outputBuilder.append("\n");
              }
            });
  }

  private void recommendInternal(Map<String, List<String>> intermediate, StringBuilder outputBuilder,
                                 String level, String track, Map<String, String> trackLevelMap,
                                 Map<String, List<String>> preRequisiteMap) {
    List<String> releases = intermediate.get(level);
    List<String> pendingReleases = getPendingReleases(getMostRecentRelease(releases));
    outputBuilder.append("\n");
    if (pendingReleases.size() == 0 ||
            pendingReleases.stream().noneMatch(release -> trackLevelMap.containsKey(release))) {
      outputBuilder.append("Congratulations. Your certification is already up-to-date!");
      outputBuilder.append("\n");
    } else {
      outputBuilder.append("Take the following courses to update your " + level + " certification: ");
      outputBuilder.append("\n");
      outputBuilder.append(pendingReleases.stream()
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
              .collect(Collectors.joining("\n")));
      outputBuilder.append("\n");
    }
    outputBuilder.append("\n\n");
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
}
