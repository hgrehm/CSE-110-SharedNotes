package edu.ucsd.cse110.sharednotes.model;

import static java.util.concurrent.TimeUnit.SECONDS;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NoteRepository {
    private final NoteDao dao;
    private final NoteAPI api;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> poller;
    private MediatorLiveData<Note> note;

    public NoteRepository(NoteDao dao) {
        this.dao = dao;
        this.api = new NoteAPI();
    }

    // Synced Methods
    // ==============

    /**
     * This is where the magic happens. This method will return a LiveData object that will be
     * updated when the note is updated either locally or remotely on the server. Our activities
     * however will only need to observe this one LiveData object, and don't need to care where
     * it comes from!
     *
     * This method will always prefer the newest version of the note.
     *
     * @param title the title of the note
     * @return a LiveData object that will be updated when the note is updated locally or remotely.
     */
    public LiveData<Note> getSynced(String title) {
        note = new MediatorLiveData<Note>();

        Observer<Note> updateFromRemote = theirNote -> {
            var ourNote = note.getValue();
            System.out.println(ourNote.content);
            if (theirNote == null) return;
            System.out.println(ourNote.version);
            if (ourNote == null || ourNote.version < theirNote.version) {
                upsertLocal(theirNote);
                System.out.println("upserting theirs");
            }
        };

        // If we get a local update, pass it on.
        note.addSource(getLocal(title), note::postValue);
        // If we get a remote update, update the local version (triggering the above observer)
        note.addSource(getRemote(title), updateFromRemote);

        return note;
    }

    public void upsertSynced(Note note) {
        System.out.println("upserting synced");
        note.version = note.version + 1;
        upsertLocal(note);
        upsertRemote(note);
    }

    // Local Methods
    // =============

    public LiveData<Note> getLocal(String title) {
        return dao.get(title);
    }

    public LiveData<List<Note>> getAllLocal() {
        return dao.getAll();

    }

    public void upsertLocal(Note note) {
        dao.upsert(note);
    }

    public void deleteLocal(Note note) {
        dao.delete(note);
    }

    public boolean existsLocal(String title) {
        return dao.exists(title);
    }

    // Remote Methods
    // ==============

    public LiveData<Note> getRemote(String title) {
        // Cancel any previous poller if it exists.
        if (this.poller != null && !this.poller.isCancelled()) {
            poller.cancel(true);
        }
        var poller = scheduler.scheduleAtFixedRate(() -> {
            Future<Note> futureNote = api.getNoteAsync(title);
            try {
                if (futureNote.get(3, SECONDS).content != null) {
                    note.postValue(futureNote.get(1, SECONDS));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 3000, TimeUnit.MILLISECONDS);

        // You may (but don't have to) want to cache the LiveData's for each title, so that
        // you don't create a new polling thread every time you call getRemote with the same title.

        return note;
    }

    public void upsertRemote(Note note) {
        System.out.println("remote "+ note.version);
        api.putNoteAsync(note);
    }
}
