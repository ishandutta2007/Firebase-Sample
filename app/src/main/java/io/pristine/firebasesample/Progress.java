package io.pristine.firebasesample;

import android.databinding.ObservableField;

public class Progress {
  public final ObservableField<String> transferred = new ObservableField<>();
  public final ObservableField<String> total = new ObservableField<>();
  public final ObservableField<String> status = new ObservableField<>();
}
