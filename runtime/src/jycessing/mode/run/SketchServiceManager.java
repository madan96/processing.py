package jycessing.mode.run;

import java.util.HashMap;
import java.util.Map;

import jycessing.mode.PyEditor;
import jycessing.mode.PythonMode;
import processing.app.Base;

public class SketchServiceManager implements ModeService {
  private static final String DEBUG_SKETCH_RUNNER_KEY = "$SKETCHRUNNER$";

  @SuppressWarnings("unused")
  private static void log(final String msg) {
    if (PythonMode.VERBOSE) {
      System.err.println(SketchServiceManager.class.getSimpleName() + ": " + msg);
    }
  }

  private final PythonMode mode;
  private final Map<String, SketchServiceProcess> sketchServices =
      new HashMap<String, SketchServiceProcess>();
  private volatile boolean isStarted = false;

  public SketchServiceManager(final PythonMode mode) {
    this.mode = mode;
  }

  public SketchServiceProcess createSketchService(final PyEditor editor) {
    final SketchServiceProcess p = new SketchServiceProcess(mode, editor);
    sketchServices.put(editor.getId(), p);
    return p;
  }

  public void destroySketchService(final PyEditor editor) {
    final SketchServiceProcess process = sketchServices.remove(editor.getId());
    if (process != null) {
      process.shutdown();
    }
  }

  public boolean isStarted() {
    return isStarted;
  }

  public void start() {
    isStarted = true;
    try {
      if (PythonMode.SKETCH_RUNNER_FIRST) {
        final ModeService stub = (ModeService)RMIUtils.export(this);
        final ModeWaiter modeWaiter = RMIUtils.lookup(ModeWaiter.class);
        modeWaiter.modeReady(stub);
      } else {
        RMIUtils.bind(this, ModeService.class);
      }
    } catch (final Exception e) {
      Base.showError("PythonMode Error", "Cannot start python sketch service.", e);
      return;
    }
  }


  private SketchServiceProcess processFor(final String editorId) {
    if (PythonMode.SKETCH_RUNNER_FIRST) {
      return sketchServices.get(DEBUG_SKETCH_RUNNER_KEY);
    }

    final SketchServiceProcess p = sketchServices.get(editorId);
    if (p == null) {
      throw new RuntimeException("I somehow got a message from the sketch runner for " + editorId
          + " but don't have an active service process for it!");
    }
    return p;
  }

  @Override
  public void handleReady(final String editorId, final SketchService service) {
    processFor(editorId).handleReady(service);
  }

  @Override
  public void handleSketchException(final String editorId, final Exception e) {
    processFor(editorId).handleSketchException(e);
  }

  @Override
  public void handleSketchStopped(final String editorId) {
    processFor(editorId).handleSketchStopped();
  }

  @Override
  public void print(final String editorId, final Stream stream, final String s) {
    processFor(editorId).print(stream, s);
  }

  @Override
  public void println(final String editorId, final Stream stream, final String s) {
    processFor(editorId).println(stream, s);
  }
}
