package com.github.dedis.popstellar.ui.lao.event.meeting;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.objects.Meeting;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MeetingRepository;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.scheduler.SchedulerProvider;
import com.github.dedis.popstellar.utility.security.KeyManager;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.Observable;
import io.reactivex.Single;
import timber.log.Timber;

@HiltViewModel
public class MeetingViewModel extends AndroidViewModel {

  public static final String TAG = MeetingViewModel.class.getSimpleName();
  private String laoId;

  private final LAORepository laoRepo;
  private final MeetingRepository meetingRepo;
  private final GlobalNetworkManager networkManager;
  private final KeyManager keyManager;
  private final SchedulerProvider schedulerProvider;

  @Inject
  public MeetingViewModel(
      @NonNull Application application,
      LAORepository laoRepo,
      MeetingRepository meetingRepo,
      GlobalNetworkManager networkManager,
      KeyManager keyManager,
      SchedulerProvider schedulerProvider) {
    super(application);
    this.laoRepo = laoRepo;
    this.meetingRepo = meetingRepo;
    this.networkManager = networkManager;
    this.keyManager = keyManager;
    this.schedulerProvider = schedulerProvider;
  }

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public Observable<Meeting> getMeetingObservable(String id) {
    try {
      return meetingRepo.getMeetingObservable(laoId, id).observeOn(schedulerProvider.mainThread());
    } catch (UnknownMeetingException e) {
      return Observable.error(new UnknownMeetingException(id));
    }
  }

  private LaoView getLao() throws UnknownLaoException {
    return laoRepo.getLaoView(laoId);
  }

  public Single<String> createNewMeeting(
      String title, String location, long creation, long proposedStart, long proposedEnd) {
    Timber.tag(TAG).d("creating a new meeting with title %s", title);

    LaoView laoView;
    try {
      laoView = getLao();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(getApplication(), TAG, e, R.string.unknown_lao_exception);
      return Single.error(new UnknownLaoException());
    }

    CreateMeeting createMeeting =
        new CreateMeeting(laoView.getId(), title, creation, location, proposedStart, proposedEnd);

    return networkManager
        .getMessageSender()
        .publish(keyManager.getMainKeyPair(), laoView.getChannel(), createMeeting)
        .toSingleDefault(createMeeting.getId());
  }
}
