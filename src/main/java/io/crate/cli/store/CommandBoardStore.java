package io.crate.cli.store;

import io.crate.cli.widgets.CommandBoardDescriptor;
import io.crate.cli.widgets.CommandBoardDescriptor.AttributeName;

import java.util.*;
import java.util.function.Function;


public class CommandBoardStore<T extends CommandBoardDescriptor> extends BaseStore<T> {

    private static final String COMMAND_BOARD_STORE_FILENAME = "command_board.properties";


    public CommandBoardStore(Function<String, T> newElementSupplier) {
        super(COMMAND_BOARD_STORE_FILENAME, newElementSupplier);
    }

    @Override
    protected Map<String, T> loadFromProperties(Properties props) {
        if (null == props) {
            return new TreeMap<>();
        }
        Map<String, T> elementsByUniqueName = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            StoreItemDescriptor.splitStoreItemAttributeKey((String) prop.getKey(), (uniqueName, attributeName) -> {
                String value = (String) prop.getValue();
                T cb = elementsByUniqueName.computeIfAbsent(uniqueName, newElementSupplier::apply);
                switch (CommandBoardDescriptor.AttributeName.valueOf(attributeName)) {
                    case connection_name:
                        cb.setSqlConnectionName(value);
                        break;

                    case board_contents:
                        cb.setBoardContents(value);
                        break;

                    default:
                        LOGGER.error("Ignoring custom command board attribute [{}.{}={}]",
                                uniqueName, attributeName, value);
                }
            });
        }
        return elementsByUniqueName;
    }

    @Override
    protected String [] producePropertiesFileContents(CommandBoardDescriptor cb) {
        return new String[]{
                toPropertiesFileFormat(cb.getStoreItemAttributeKey(AttributeName.connection_name), cb.getSqlConnectionName()),
                toPropertiesFileFormat(cb.getStoreItemAttributeKey(AttributeName.board_contents), cb.getBoardContents())
        };
    }
}
