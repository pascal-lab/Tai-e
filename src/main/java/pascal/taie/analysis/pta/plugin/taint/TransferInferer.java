package pascal.taie.analysis.pta.plugin.taint;

class TransferInferer extends OnFlyHandler {

    TransferInferer(HandlerContext context) {
        super(context);
    }

    @Override
    public void onBeforeFinish() {

    }
}
