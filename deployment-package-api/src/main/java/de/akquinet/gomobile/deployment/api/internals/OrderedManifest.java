package de.akquinet.gomobile.deployment.api.internals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class OrderedManifest extends Manifest {

    private Manifest manifest;

    public OrderedManifest() {
        super();
        manifest = new Manifest();
        // HACK
        Field[] fields = manifest.getClass().getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals("entries")) {
                // Found !
                try {
                    f.setAccessible(true);
                    f.set(manifest, new OrderedMap<String, Attributes>());
                } catch (Exception e) {
                    throw new RuntimeException(
                            "Cannot created the ordered manifest : " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void clear() {
        manifest.clear();
    }

    @Override
    public Object clone() {
        return manifest.clone();
    }

    @Override
    public boolean equals(Object o) {
       return manifest.equals(o);
    }

    @Override
    public Attributes getAttributes(String name) {
        return manifest.getAttributes(name);
    }

    @Override
    public Map<String, Attributes> getEntries() {
        return manifest.getEntries();
    }

    @Override
    public Attributes getMainAttributes() {
        return manifest.getMainAttributes();
    }

    @Override
    public int hashCode() {
        return manifest.hashCode();
    }

    @Override
    public void read(InputStream is) throws IOException {
        manifest.read(is);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        manifest.write(out);
    }

    class OrderedMap<String, Attributes> extends HashMap<String, Attributes> {

        private static final long serialVersionUID = 3725974338131428785L;

        List<String> sections = new ArrayList<String>();

        @Override
        public Set<String> keySet() {
            // Should follow the same order.
            Set<String> set = new HashSet<String>(sections);
            return set;
        }

        @Override
        public Set<Entry<String, Attributes>> entrySet() {

            Set<Entry<String, Attributes>> set = new AbstractSet<Entry<String, Attributes>>() {

                @Override
                public Iterator<Entry<String, Attributes>> iterator() {
                    return new Iterator<Entry<String, Attributes>>() {

                        Iterator<String> sectionIterator = sections.iterator();

                        public boolean hasNext() {
                            return sectionIterator.hasNext();
                        }

                        public Entry<String, Attributes> next() {
                            final String section = sectionIterator.next();
                            for(Entry<String, Attributes> entry : OrderedMap.this.getMyEntrySet()) {
                                if (entry.getKey().equals(section)) {
                                    return entry;
                                }
                            }
                            // Should not happen...
                            return null;
                        }

                        public void remove() {
                            throw new UnsupportedOperationException();
                        }

                    };
                }

                @Override
                public int size() {
                    return sections.size();
                }

            };

            return set;


        }

        protected Set<Entry<String, Attributes>> getMyEntrySet() {
           return super.entrySet();
        }

        @Override
        public Attributes put(String key, Attributes value) {
            if (!sections.contains(key)) {
                sections.add(key); // Add in queue.
            }
            return super.put(key, value);
        }

    }



}
