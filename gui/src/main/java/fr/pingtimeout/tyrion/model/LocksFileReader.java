package fr.pingtimeout.tyrion.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LocksFileReader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    private final Logger logger;

    private final Set<Access> criticalSections;


    public LocksFileReader(File file) {
        logger = Logger.getLogger(getClass().getName());
        criticalSections = new TreeSet<>();

        try (InputStream inputStream = new FileInputStream(file)) {
            loadFile(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load file " + file, e);
        }
    }

    public LocksFileReader(InputStream inputStream) {
        logger = Logger.getLogger(getClass().getName());
        criticalSections = new TreeSet<>();

        loadFile(inputStream);
    }


    private void loadFile(InputStream inputStream) {
        final Map<Accessor, CriticalSectionEntered> lastEnterInCriticalSection = new HashMap<>();

        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                processLine(line, lastEnterInCriticalSection);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not load file", e);
        }
    }


    private void processLine(String line, Map<Accessor, CriticalSectionEntered> lastEnterInCriticalSection) throws IOException {
        if (line.startsWith("{\"enter\":")) {
            CriticalSectionEntered event = OBJECT_MAPPER.readValue(line, CriticalSectionEntered.class);
            process(event, lastEnterInCriticalSection);
        } else if (line.startsWith("{\"exit\":")) {
            CriticalSectionExit event = OBJECT_MAPPER.readValue(line, CriticalSectionExit.class);
            process(event, lastEnterInCriticalSection);
        } else {
            // TODO: for the time being we will ignore "entering" event
            logger.warning("Unknown line: " + line);
        }
    }


    private void process(CriticalSectionExit exit, Map<Accessor, CriticalSectionEntered> lastEnterInCriticalSection) {
        Accessor accessor = exit.getAccessor();
        CriticalSectionEntered lastEnter = lastEnterInCriticalSection.get(accessor);

        if (lastEnter == null) {
            logger.log(Level.FINE, "Got 'exit' without matching 'enter', ignoring event", exit);
        } else {
            Access criticalSection = new Access(
                    lastEnter.getTimestamp(), exit.getTimestamp(),
                    accessor,
                    lastEnter.getTarget());

            criticalSections.add(criticalSection);
            lastEnterInCriticalSection.remove(accessor);
        }
    }


    private void process(CriticalSectionEntered enter, Map<Accessor, CriticalSectionEntered> lastEnterInCriticalSection) {
        Accessor accessor = enter.getAccessor();
        CriticalSectionEntered lastEnter = lastEnterInCriticalSection.get(accessor);

        if (lastEnter == null) {
            lastEnterInCriticalSection.put(accessor, enter);
        } else {
            logger.log(Level.FINE, "Got two consecutive 'enter', ignoring the first one", enter);
        }
    }


    public AccessReport buildAccessReport() {
        return new AccessReport(criticalSections);
    }
}