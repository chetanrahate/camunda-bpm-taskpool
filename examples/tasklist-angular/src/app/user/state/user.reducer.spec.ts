import {AvailableUsersLoadedAction, LoadAvailableUsersAction, SelectUserAction, UserProfileLoadedAction} from './user.actions';
import {UserProfile, userReducer, UserState} from './user.reducer';

describe('processReducer', () => {

  const initialState: UserState = {
    currentUserId: null,
    currentUserProfile: {
      fullName: '',
      username: '',
      userIdentifier: ''
    },
    availableUserIds: []
  };

  it('updates available users', () => {
    // given:
    const availableIds = ['foo', 'bar'];
    const action = new AvailableUsersLoadedAction(availableIds);

    // when:
    const newState = userReducer(initialState, action);

    // then:
    expect(newState.availableUserIds).toBe(availableIds);
  });

  it('updates current user', () => {
    // given:
    const currentUser: UserProfile = {
      username: 'foo',
      userIdentifier: 'bar',
      fullName: 'foobar'
    };
    const action = new UserProfileLoadedAction(currentUser);

    // when:
    const newState = userReducer(initialState, action);

    // then:
    expect(newState.currentUserProfile).toBe(currentUser);
  });

  it('ignores other actions', () => {
    // given:
    const action = new LoadAvailableUsersAction();

    // when:
    const newState = userReducer(initialState, action);

    // then:
    expect(newState).toBe(initialState);
  });

  it('updates current userId', () => {
    // given:
    const action = new SelectUserAction('kermit');

    // when:
    const newState = userReducer(initialState, action);

    // then:
    expect(newState.currentUserId).toEqual('kermit');
  });
});