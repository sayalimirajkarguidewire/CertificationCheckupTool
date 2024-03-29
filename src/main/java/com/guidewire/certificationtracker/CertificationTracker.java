package com.guidewire.certificationtracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static List<String> RELEASE_ORDER = Arrays.asList("10.0", "10.x", "Aspen", "Banff", "Cortina", "Dobson", "Elysian", "Flaine",
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
      .filter(row -> {
        String courseName = preprocessCourseName(row[5]);
        if (getReleaseNumber(courseName) > 0 && getReleaseNumber(courseName) < 10) {
          return false;
        }
        return true;
      })
      .forEach(row -> {
        try {
          String email = row[1];
          if (email.equals("bkrishna@germaniainsurance.com")) {
            System.out.println("Hdagsd");
          }
          String courseName = preprocessCourseName(row[5]);
          String[] courseNameParts = courseName.split("-�|-");
          String level = normalize(getAlphaNumericString(courseNameParts[0].trim())).trim();
          String track = normalize(getAlphaNumericString(courseNameParts[1].trim())).trim();
          String release = normalize(getAlphaNumericString(courseNameParts[2].trim())).trim();
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
          System.out.println("Failed to read row: " + Stream.of(row).reduce((a, b) -> a + " " + b).orElse(""));
        }
      });
  }

  private int getReleaseNumber(String input) {
    Matcher m = Pattern.compile("\\d+").matcher(input);
    while (m.find()) {
      return Integer.parseInt(m.group(0));
    }
    return -1;
  }

  private String preprocessCourseName(String courseName) {
    if (courseName.contains("10.0")) {
      courseName = courseName.replaceAll("\\s*10.0\\s*", " ");
      courseName = courseName + " - 10.0";
      courseName = courseName.replaceAll("Associate Certification", "Guidewire Certified Associate");
      courseName = courseName.replaceAll("Ace Certification", "Guidewire Certified Ace");
      courseName = courseName.replaceAll("Specialist Certification", "Guidewire Certified Specialist");
      courseName = courseName.replaceAll("Professional Certification", "Guidewire Certified Professional");
    } else if (courseName.contains("10.x")) {
      courseName = courseName.replaceAll("\\s*10.x\\s*", " ");
      courseName = courseName + " - 10.x";
      courseName = courseName.replaceAll("Associate Certification", "Guidewire Certified Associate");
      courseName = courseName.replaceAll("Ace Certification", "Guidewire Certified Ace");
      courseName = courseName.replaceAll("Specialist Certification", "Guidewire Certified Specialist");
      courseName = courseName.replaceAll("Professional Certification", "Guidewire Certified Professional");
    }
    courseName = courseName.replaceAll("Digital Configuration", "EnterpriseEngage Configuration");
    courseName = courseName.replaceAll("Digital Integration", "EnterpriseEngage Integration");
    courseName = courseName.replaceAll("DataHub Integration", "DataHub and InfoCenter Integration");
    courseName = courseName.replaceAll("InfoCenter Integration", "DataHub and InfoCenter Integration");
    courseName = courseName.replaceAll("DataHub and DataHub", "DataHub");
    return courseName;
  }

  private String normalize(String input) {
    return input.replaceAll("[^a-zA-Z0-9-\\.\\s+]", " ");
  }

  private String getAlphaNumericString(String input) {
    int startIndex = 0;
    for (int i = 0; i < input.length(); i++) {
      if (Character.isAlphabetic(input.charAt(i)) || Character.isDigit(input.charAt(i)) || input.charAt(i) == '.') {
        startIndex = i;
        break;
      }
    }
    return input.substring(startIndex);
  }

  public void analyzeDataForAllUsers(String outputFilePath) throws Exception {
    List<String> outputLines = new ArrayList<>();
    outputLines.add("User Email,Organization Name,Manager Email,Recommendation");
    List<String> contentLines = this.emailToNameMap.entrySet()
            .stream()
            .map(entry -> {
              try {
                String recommendations = this.getRecommendations(entry.getKey());
                return entry.getKey() + ",\"" + emailToOrganizationMap.get(entry.getKey()) + "\",\""
                        + emailToManagerMap.get(entry.getKey()) + "\"," + "\"" + recommendations + "\"";
              } catch (Exception e) {
                System.out.println("Exception occurred : " + e.getStackTrace());
              }
              return "";
            })
            .map(line -> line.replaceAll("<br>", "\n").replaceAll("<[^>]*>", ""))
            .collect(Collectors.toList());
    outputLines.addAll(contentLines);
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
        outputBuilder.append("Unknown Track : " + track + "<br><br>");
      } else {
        String linkedAssociateTrack = trackCertificationRules.rules.get(track).linkedAssociateTrack;
        Map<String, String> linkedAssociateTrackLevelMap = linkedAssociateTrack != null
                ? trackCertificationRules.rules.get(linkedAssociateTrack).associateMap
                : Collections.emptyMap();
        boolean isValidLevel = false;
        Pair<Boolean, List<String>> linkedAssociateTrackStatus = null;
        if (personMap.containsKey(linkedAssociateTrack)) {
          linkedAssociateTrackStatus = recommendInternal(personMap.get(linkedAssociateTrack), new StringBuilder(), "Guidewire Certified Associate", "Guidewire Certified Associate", linkedAssociateTrack,
                  Collections.emptyMap(),
                  Collections.emptyMap(),
                  trackCertificationRules.rules.get(linkedAssociateTrack).preRequisiteMap, true);
        }
        if (intermediate.containsKey("Guidewire Certified Associate")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Associate", "Guidewire Certified Associate", track,
                  trackCertificationRules.rules.get(track).associateMap,
                  linkedAssociateTrackLevelMap,
                  trackCertificationRules.rules.get(track).preRequisiteMap, false);
          isValidLevel = true;
        }
        if (intermediate.containsKey("Guidewire Certified Ace")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Ace", "Ace Certification", track,
                  trackCertificationRules.rules.get(track).otherMap,
                  getEffectiveLinkedMap(linkedAssociateTrackLevelMap, linkedAssociateTrackStatus),
                  trackCertificationRules.rules.get(track).preRequisiteMap, false);
          isValidLevel = true;
        } else if (intermediate.containsKey("Guidewire Certified Specialist")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Specialist", "Specialist Certification", track,
                  trackCertificationRules.rules.get(track).otherMap,
                  getEffectiveLinkedMap(linkedAssociateTrackLevelMap, linkedAssociateTrackStatus),
                  trackCertificationRules.rules.get(track).preRequisiteMap, false);
          isValidLevel = true;
        } else if (intermediate.containsKey("Guidewire Certified Professional")) {
          recommendInternal(intermediate, outputBuilder, "Guidewire Certified Professional", "Professional Certification", track,
                  trackCertificationRules.rules.get(track).otherMap,
                  getEffectiveLinkedMap(linkedAssociateTrackLevelMap, linkedAssociateTrackStatus),
                  trackCertificationRules.rules.get(track).preRequisiteMap, false);
          isValidLevel = true;
        }
        if (!isValidLevel) {
          outputBuilder.append("Unknown Level : " + Arrays.toString(intermediate.keySet().toArray()) + "<br><br><br>");
        }
        hasValidLevel.set(isValidLevel);
      }
    });
    if (hasValidLevel.get()) {
      outputBuilder.append("<br><br>To access courses for updating certifications log into Guidewire Education "
        + "account and go to My Learning -> My Learning Paths -> New Features Subscription. Complete the courses" +
              " in the specified order to update your certification.<br><br>");
    }
    outputBuilder.append("</body></html>");
    return outputBuilder.toString();
  }

  private Map<String, String> getEffectiveLinkedMap(Map<String, String> inputMap,
                                                    Pair<Boolean, List<String>> linkedAssociateTrackStatus) {
    if (linkedAssociateTrackStatus == null) {
      return inputMap;
    }
    return linkedAssociateTrackStatus.first ? Collections.emptyMap() : getSubMap(inputMap, linkedAssociateTrackStatus.second);
  }

  private Map<String, String> getSubMap(Map<String, String> input, List<String> pendingReleases) {
    Map<String, String> subMap = new HashMap<>();
    pendingReleases.stream()
            .filter(rel -> input.containsKey(rel))
            .forEach(rel -> subMap.put(rel, input.get(rel)));
    return subMap;
  }

  private void outputCurrentCertifications(String track,
                                           Map<String, List<String>> intermediate, StringBuilder outputBuilder) {
    outputBuilder.append("<b>Current certifications for Track: " + track + "</b><br>");
    intermediate.entrySet().stream()
            .forEach(entry -> {
              if (entry.getKey().equals("Guidewire Certified Associate")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                if (mostRecentRelease.equals("10.0") || mostRecentRelease.equals("10.x")) {
                  outputBuilder.append("Associate Certification" + " - " + track + " - " + mostRecentRelease);
                } else {
                  outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                }
                outputBuilder.append("<br>");
              }
              if (entry.getKey().equals("Guidewire Certified Ace")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                if (mostRecentRelease.equals("10.0") || mostRecentRelease.equals("10.x")) {
                  outputBuilder.append("Ace Certification" + " - " + track + " - " + mostRecentRelease);
                } else {
                  outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                }
                outputBuilder.append("<br>");
              } else if (entry.getKey().equals("Guidewire Certified Specialist")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                if (mostRecentRelease.equals("10.0") || mostRecentRelease.equals("10.x")) {
                  outputBuilder.append("Specialist Certification" + " - " + track + " - " + mostRecentRelease);
                } else {
                  outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                }
                outputBuilder.append("<br>");
              } else if (entry.getKey().equals("Guidewire Certified Professional")) {
                String mostRecentRelease = getMostRecentRelease(entry.getValue());
                if (mostRecentRelease.equals("10.0") || mostRecentRelease.equals("10.x")) {
                  outputBuilder.append("Professional Certification" + " - " + track + " - " + mostRecentRelease);
                } else {
                  outputBuilder.append(entry.getKey() + " - " + track + " - " + mostRecentRelease);
                }
                outputBuilder.append("<br>");
              }
            });
  }

  private Pair<Boolean, List<String>> recommendInternal(Map<String, List<String>> intermediate, StringBuilder outputBuilder,
                            String level, String altLevel, String track, Map<String, String> trackLevelMap,
                            Map<String, String> linkedAssociateTrackLevelMap,
                            Map<String, List<String>> preRequisiteMap,
                            boolean isForLinkedAssociateCheck) {
    List<String> releases = intermediate.getOrDefault(level, intermediate.get(altLevel));
    String mostRecentRelease = getMostRecentRelease(releases);
    if (!isValidRelease(mostRecentRelease)) {
      outputBuilder.append("Unknown Release : " + mostRecentRelease + "<br>");
      return Pair.pair(false, Arrays.asList());
    }
    boolean isUpToDate = false;
    List<String> pendingReleases = getPendingReleases(mostRecentRelease);
    if (pendingReleases.size() == 0 ||
            (!isForLinkedAssociateCheck && pendingReleases.stream().noneMatch(release -> trackLevelMap.containsKey(release)))) {
      outputBuilder.append("<b><i style=\"color:green;\">Your certification is already up-to-date!</i></b>");
      isUpToDate = true;
    } else {
      outputBuilder.append("<b><i style=\"color:blue;\">Take the following courses to update your " + level + " certification:</i></b>");
      outputBuilder.append("<br>");

      outputBuilder.append(Stream.of(getUpdateMessage(pendingReleases, linkedAssociateTrackLevelMap, Collections.emptyMap()),
              getUpdateMessage(pendingReleases, trackLevelMap, preRequisiteMap))
              .filter(x -> !x.trim().isEmpty())
              .collect(Collectors.joining("<br>")));
    }
    outputBuilder.append("<br><br>");
    return Pair.pair(isUpToDate, pendingReleases);
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
    return RELEASE_ORDER.subList(startIndex + 1, RELEASE_ORDER.size())
            .stream()
            .filter(x -> !x.equals("10.0") && !x.equals("10.x"))
            .collect(Collectors.toList());
  }

  public String getNameFromEmail(String email) {
    return emailToNameMap.get(email).split(" ")[0];
  }

  public String getFullNameFromEmail(String email) {
    return emailToNameMap.get(email);
  }
}
