package states.actions;

import model.User;
import states.SearchLsState;
import utils.LangMap;

import static utils.TextUtils.notNull;

/**
 * @author Denis Danilin | denis@danilin.name
 * 26.05.2020
 * tfs ☭ sweat and blood
 */
public class SearchAction extends AInputAction {
    public static final String NAME = SearchAction.class.getSimpleName();

    public SearchAction() {
        super(NAME);
    }

    @Override
    void onInputInternal(final String input, final User user) {
        fsService.search(notNull(input), user);
        user.setSearchOffset(0);
        user.setState(SearchLsState.NAME);
    }


    @Override
    void askInternal(final User user) {
        user.setQuery(null);
        gui.dialog(LangMap.Value.TYPE_QUERY, user);
    }
}
