import Foundation

let args = CommandLine.arguments
let cmd = args.count > 1 ? args[1] : "all"

switch cmd {
case "did-doc":
    DidDocBench.run()
case "vc":
    VcBench.run()
case "key-agreement":
    KeyAgreementBench.run()
case "all":
    DidDocBench.run()
    print()
    VcBench.run()
    print()
    KeyAgreementBench.run()
case "scenario":
    do {
        try Scenario.run()
    } catch {
        FileHandle.standardError.write(Data("Scenario failed: \(error)\n".utf8))
        exit(1)
    }
default:
    FileHandle.standardError.write(
        Data("Usage: pqc-bench [did-doc|vc|key-agreement|all|scenario]\n".utf8)
    )
    exit(1)
}
