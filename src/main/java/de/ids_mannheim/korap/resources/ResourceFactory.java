package de.ids_mannheim.korap.resources;

import de.ids_mannheim.korap.exceptions.KustvaktException;
import de.ids_mannheim.korap.exceptions.StatusCodes;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class ResourceFactory {

    public static final List<Class<? extends KustvaktResource>> subTypes = new ArrayList<>();
    public static final int CORPUS = 0;
    public static final int FOUNDRY = 1;
    public static final int LAYER = 2;
    public static final int VIRTUALCOLLECTION = 3;
    public static final int USERQUERY = 4;

    static {
        subTypes.add(CORPUS, Corpus.class);
        subTypes.add(FOUNDRY, Foundry.class);
        subTypes.add(LAYER, Layer.class);
        subTypes.add(VIRTUALCOLLECTION, VirtualCollection.class);
        //        subTypes.add(USERQUERY, UserQuery.class);
    }


    public static KustvaktResource getResource (
            Class<? extends KustvaktResource> clazz) {
        try {
            return (KustvaktResource) clazz.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e) {
            // do nothing
        }
        return null;
    }


    public static int getResourceMapping (Class<? extends KustvaktResource> r) {
        int value = -1;
        if (r != null) {
            for (int i = 0; i < subTypes.size(); i++) {
                if (subTypes.get(i).getName().equals(r.getName()))
                    value = i;
            }
        }
        return value;
    }


    public static KustvaktResource getResource (String type) throws KustvaktException {
        return getResource(getResourceClass(type));
    }


    public static KustvaktResource getResource (int j) {
        Class s = subTypes.get(j);
        if (s != null) {
            return getResource(s);
        }
        return null;
    }


    public static <T extends KustvaktResource> T createID (T resource) {
        if (resource.getData() != null && !resource.getStringData().isEmpty())
            resource.setPersistentID(DigestUtils.sha1Hex(resource
                    .getStringData()));
        return resource;
    }


    public static <T extends KustvaktResource> Class<T> getResourceClass (
            String type) throws KustvaktException {
        for (Class value : subTypes) {
            if (value == VirtualCollection.class
                    && type.equalsIgnoreCase("collection"))
                return (Class<T>) VirtualCollection.class;
            //todo
            //            else if (value == UserQuery.class && type.equalsIgnoreCase("query"))
            //                return (Class<T>) UserQuery.class;
            else if (value.getSimpleName().equalsIgnoreCase(type.trim())) {
                return value;
            }
        }
        // todo: throw exception in case of missing parameter!
        throw new KustvaktException(StatusCodes.ILLEGAL_ARGUMENT, "resource type could not be identified!");
    }


    // all deprecated!

    public static VirtualCollection getCachedCollection (String query) {
        VirtualCollection v = new VirtualCollection(query);
        v.setName("");
        v.setDescription("");
        return v;
    }


    public static VirtualCollection getPermanentCollection (
            VirtualCollection mergable, String corpusName, String description) {
        VirtualCollection v = new VirtualCollection();
        v.merge(mergable);
        v.setName(corpusName);
        v.setDescription(description);
        return createID(v);
    }


    //    public static VirtualCollection createCollection(String name, String query,
    //            Integer owner) {
    //        VirtualCollection v = new VirtualCollection(query);
    //        v.setName(name);
    //        v.setOwner(owner);
    //        return v;
    //    }
    //
    //    public static VirtualCollection createCollection(String name,
    //            Integer owner) {
    //        VirtualCollection v = new VirtualCollection();
    //        v.setOwner(owner);
    //        v.setName(name);
    //        return v;
    //    }
    //
    //    public static VirtualCollection getCollection(Integer collectionID,
    //            boolean cache) {
    //        VirtualCollection v = new VirtualCollection();
    //        v.setId(collectionID);
    //        v.setDescription("");
    //        v.setName("");
    //        return v;
    //    }
    //
    //    public static VirtualCollection createContainer(String name,
    //            String description, String query, Integer owner) {
    //        VirtualCollection v = new VirtualCollection(query);
    //        v.setName(name);
    //        v.setDescription(description);
    //        v.setOwner(owner);
    //        v.setManaged(true);
    //        return v;
    //    }

    public static VirtualCollection getIDContainer (Integer id) {
        return new VirtualCollection(id);
    }
}
