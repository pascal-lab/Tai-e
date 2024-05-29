package pascal.taie.android.info;

import java.util.Set;

public record TransferFilterInfo(Set<String> classNames,
                                 Set<String> actions,
                                 Set<String> categories,
                                 Set<UriData> data) {

    public boolean emptyImplicitFilterInfo() {
        return actions.isEmpty() && categories.isEmpty() && data.isEmpty();
    }
}
