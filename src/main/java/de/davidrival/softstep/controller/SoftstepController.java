package de.davidrival.softstep.controller;

import com.bitwig.extension.api.util.midi.ShortMidiMessage;
import com.bitwig.extension.controller.api.Parameter;
import de.davidrival.softstep.api.SendToApi;
import de.davidrival.softstep.hardware.SoftstepHardware;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
public class SoftstepController {

    private static final int MODE_THRESHOLD = 0;

    Pages currentPage;

    private SoftstepHardware softstepHardware;

    final Softstep1Controls controls = new Softstep1Controls();

    private SendToApi sendToApi;

    public void display() {
        softstepHardware.displayText(currentPage.name());
        softstepHardware.showLeds(currentPage);
    }
    public void handleMidi(ShortMidiMessage msg) {
        checkForPageChange(msg);
        controls.update(msg);
        checkApiToBitwig();
    }

    private void checkApiToBitwig() {
        List<Softstep1Pad> pads = controls.getPads().stream().filter(pad -> pad.hasChanged).collect(Collectors.toList());

        // todo check mode aka page
        pads.forEach(pad -> {
                    Parameter param = sendToApi.userControls.getControl(pad.getNumber());
                    param.set(pad.pressure, 128);
                    pad.hasChanged = false;
                }
        );
    }

    private void checkForPageChange(ShortMidiMessage msg) {
        if ( msg.getStatusByte() == 176 && msg.getData1() == 80 && msg.getData2() > MODE_THRESHOLD) {
            if (currentPage.pageIndex != Pages.CLIP.pageIndex) {
                currentPage = Pages.CLIP;
                display();
            }
        }
        if ( msg.getStatusByte() == 176 && msg.getData1() == 81 && msg.getData2() > MODE_THRESHOLD ) {
            if (currentPage.pageIndex != Pages.CTRL.pageIndex) {
                currentPage = Pages.CTRL;
                display();
            }
        }
    }

    public void exit() {
        softstepHardware.exit();
    }

}