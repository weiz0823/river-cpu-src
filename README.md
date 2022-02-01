# River -- A RISC-V CPU (SpinalHDL source)

2021 fall computer organization and design course project.

## Build

This project is built using [Mill](com-lihaoyi.github.io/mill/).
Run `mill River`, and you will get 'generated_verilog/River/thinpad_top.v'.

## Simulation

Sim code is in [River/src/sim](River/src/sim).
For example, if you want to run
[SramControllerSim](River/src/sim/SramControllerSim.scala),
run `mill River.runMain sim.SramControllerSim`.
You will get wave in 'simWorkspace/SramController/'.

## Data models

'*BusBundle' incicate a bus bundle. The handshake mechanism is kind of like
[AXI bus](https://en.wikipedia.org/wiki/Advanced_eXtensible_Interface#Handshake).
But the bus model originated from Wishbone bus, so the 'valid' signal is named 'stb',
and the 'ready' signal is named 'ack'.

## Scala-metals language server support

This project uses
[Bloop build server with Mill](https://scalacenter.github.io/bloop/docs/build-tools/mill)
for Metals. If Metals 'Import build' failed, try to install bloop first, and run 'mill mill.contrib.Bloop/install'. Check with `bloop projects`, you should see `River`.

## Docs

[简要设计文档](doc_zh-cn.md)

详细设计文档在`doc/`目录下（未完成）。

## RiverOS

RiverOS可以用来模拟River CPU运行操作系统，只需要指定SBI的bin文件和OS的bin文件，详见[sim.sh](sim.sh)中的示例。
