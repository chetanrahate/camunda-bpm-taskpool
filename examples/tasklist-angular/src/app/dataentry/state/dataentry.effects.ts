import {Injectable} from '@angular/core';
import {Actions, Effect, ofType} from '@ngrx/effects';
import {UserStoreService} from 'app/user/state/user.store-service';
import {ArchiveService} from 'tasklist/services';
import {DataEntriesLoaded, DataEntryActionTypes, LoadDataEntries} from 'app/dataentry/state/dataentry.actions';
import {flatMap, map, withLatestFrom} from 'rxjs/operators';
import {DataEntry as DataEntryDto} from 'tasklist/models';
import {DataEntry} from 'app/dataentry/state/dataentry.reducer';
import {UserActionTypes} from 'app/user/state/user.actions';

@Injectable()
export class DataentryEffects {

  public constructor(
    private archiveService: ArchiveService,
    private userStore: UserStoreService,
    private actions$: Actions) {
  }

  @Effect()
  loadDataEntriesOnUserSelect = this.actions$.pipe(
    ofType(UserActionTypes.SelectUser),
    map(() => new LoadDataEntries())
  );

  @Effect()
  loadDataEntries$ = this.actions$.pipe(
    ofType(DataEntryActionTypes.LoadDataEntries),
    withLatestFrom(this.userStore.userId$()),
    flatMap(([_, userId]) => this.archiveService.getBosResponse({
      page: 0,
      size: 100,
      sort: '',
      filter: [''],
      XCurrentUserID: userId
    })),
    map(dataEntriesDtos => mapFromDto(dataEntriesDtos)),
    map(dataEntries => new DataEntriesLoaded(dataEntries))
  );
}

function mapFromDto(dataEntryDtos: DataEntryDto[]): DataEntry[] {
  return dataEntryDtos.map(dto => {
    return {
      name: dto.name,
      url: dto.url,
      description: dto.description,
      type: dto.type,
      payload: dto.payload
    };
  });
}